package Translate;

import Tree.*;

public class Translate {
    private Frag.Frag frags = null;
    public Frame.Frame frame = null;

    // Translate输入一个 Frame 返回一个段链表
    public Translate(Frame.Frame f) {
        frame = f;
    }

    public Frag.Frag getResult() {
        return frags;
    }

    public void addFrag(Frag.Frag frag) {
        // 增长段列表
        frag.next = frags;
        frags = frag;
    }

    public void procEntryExit(Level level, Exp body, boolean returnValue) {
        Stm b = null;
        if (returnValue) {
            // 若有返回值，则将返回值存入 $v0
            b = new MOVE(new TEMP(level.frame.RV()), body.unEx());
        } else {
            // 若无返回值，则转换为Nx
            b = body.unNx();
        }
        // 加入函数的入口和出口代码
        b = level.frame.procEntryExit1(b);
        // 增加程序段
        addFrag(new Frag.ProcFrag(b, level.frame));
    }

    public Exp transNoExp() {
        // 产生一条空语句，实际上和 nil 语句是同一个效果
        return new Ex(new CONST(0));
    }

    public Exp transIntExp(int value) {
        // 翻译整型常数
        return new Ex(new CONST(value));
    }

    public Exp transStringExp(String string) {
        // 翻译字符串常量，产生一个新的数据段
        Temp.Label lab = new Temp.Label();
        addFrag(new Frag.DataFrag(lab, frame.string(lab, string)));
        return new Ex(new NAME(lab));
    }

    public Exp transNilExp() {
        // 翻译 nil 语句
        return new Ex(new CONST(0));
    }

    public Exp transStringRelExp(Level currentL, int oper, Exp left, Exp right) {
        // 翻译字符串比较运算，调用标准库函数进行比较，然后进行逻辑判断
        Tree.Exp comp = currentL.frame.externalCall("stringEqual",
                new ExpList(left.unEx(), new ExpList(right.unEx(), null)));
        return new RelCx(oper, new Ex(comp), new Ex(new CONST(1)));
    }

    public Exp transAssignExp(Exp lvalue, Exp exp) {
        // 左值和右值都翻译成exp，赋值表达式没有返回值
        return new Nx(new MOVE(lvalue.unEx(), exp.unEx()));
    }

    public Exp transStdCallExp(Level currentL, Temp.Label name, java.util.ArrayList<Exp> args_value) {
        // 翻译调用标准库函数
        ExpList args = null;
        for (int i = args_value.size() - 1; i >= 0; --i) {
            args = new ExpList(((Exp)args_value.get(i)).unEx(), args);
        }
        // 与普通函数调用的区别在于标准库函数不存在函数嵌套定义，即不存在静态链
        return new Ex(currentL.frame.externalCall(name.toString(), args));
    }

    public Exp transOpExp(int oper, Exp left, Exp right) {
        // 翻译二元运算
        if (oper >= BINOP.PLUS && oper <= BINOP.DIV) {
            // 加减乘除运算
            return new Ex(new BINOP(oper, left.unEx(), right.unEx()));
        }

        // 逻辑判断
        return new RelCx(oper, left, right);
    }

    public Exp transCallExp(Level currentL, Level dest, Temp.Label name, java.util.ArrayList<Exp> args_value) {
        // 翻译普通函数调用
        ExpList args = null;
        for (int i = args_value.size() - 1; i >= 0; --i) {
            args = new ExpList(((Exp)args_value.get(i)).unEx(), args);
        }
        // 产生实参参数列表
        // currentL：当前层
        Level l = currentL;
        // 当前的帧
        Tree.Exp currentFP = new TEMP(l.frame.FP());
        while (dest.parent != l) {
            // 每当调用函数 f 时，便传递给 f 一个指针，该指针指向静态包含 f 的那个函数，称这个指针为静态链
            // 这个循环相当一层一层往上找，直到找到包含 dest 的层
            currentFP = l.staticLink().acc.exp(currentFP);
            l = l.parent;
        }
        // 搜索逃逸信息找到静态链所指向的层
        args = new ExpList(currentFP, args);
        // 根据逃逸信息产生逃逸信息，并作为第一个参数即 $a0 传入函数
        return new Ex(new CALL(new NAME(name), args));
    }

    public Exp transRecordExp(Level currentL, java.util.ArrayList<Exp> field) {
        // 调用外部函数 _allocRecord 为 RECORD 在 frame 上分配空间，
        // 并得到存储空间的首地址
        // _allocRecord 执行如下的类 C 代码，注意它只负责分配空间
        // 初始化操作需要我们来完成
        // 以下是 runtime.c 中的代码
        // # int *allocRecord(int size) {
        // #   int i;
        // #   int *p, *a;
        // #   p = a = (int *)malloc(size);
        // #   for (i = 0; i < size; i += sizeof(int)) *p++ = 0;
        // #   return a;
        // # }
        // 注意如果记录record为空，也要用 1 个机器字，否则每个域为一个机器字，按顺序存放
        Temp.Temp addr = new Temp.Temp();
        Tree.Exp rec_id = currentL.frame.externalCall("allocRecord",
                new ExpList(new CONST((field.size() == 0 ? 1 : field.size()) * Library.WORDSIZE), null));

        Stm stm = transNoExp().unNx();
        // 初始化指令
        for (int i = field.size() - 1; i >= 0; --i) {
            Tree.Exp offset = new BINOP(BINOP.PLUS, new TEMP(addr), new CONST(i * Library.WORDSIZE));
            Tree.Exp value = (field.get(i)).unEx();
            // 为记录中每个域生成 MOVE 指令，将值复制到帧中的相应区域
            stm = new SEQ(new MOVE(new MEM(offset), value), stm);
        }
        // 返回记录的首地址
        return new Ex(new ESEQ(new SEQ(new MOVE(new TEMP(addr), rec_id), stm), new TEMP(addr)));
    }

    public Exp transArrayExp(Level currentL, Exp init, Exp size) {
        // 调用外部函数 initArray 为数组在 frame 上分配存储空间
        // 并得到存储空间首地址
        // initArray 执行如下的类 C 代码，需要提供数组大小和初始值
        // # int *initArray(int size, int init) {
        // #   int i;
        // #   int *a = (int *)malloc(size * sizeof(int));
        // #   for (i = 0; i < size; i++) a[i] = init;
        // #   return a;
        // # }
        Tree.Exp alloc = currentL.frame.externalCall("initArray",
                new ExpList(size.unEx(), new ExpList(init.unEx(), null)));
        return new Ex(alloc);
    }

    // 将 if 语句翻译为 IR 树的节点
    public Exp transIfExp(Exp test, Exp e1, Exp e2) {
        return new IfExp(test, e1, e2);
    }

    // 将 while 语句翻译为 IR 树的节点，注意只可能是 Nx
    public Exp transWhileExp(Exp test, Exp body, Temp.Label out) {
        return new WhileExp(test, body, out);
    }

    // 将 for 语句翻译为 IR 树的节点，注意只可能是 Nx
    public Exp transForExp(Level currentL, Access var, Exp low, Exp high, Exp body, Temp.Label out) {
        return new ForExp(currentL, var, low, high, body, out);
    }

    // 翻译 break 语句为 IR 树的节点，l 为 loopstack 的栈顶标记
    public Exp transBreakExp(Temp.Label l) {
        return new Nx(new JUMP(l));
    }

    // 翻译简单变量
    public Exp transSimpleVar(Access acc, Level currentL) {
        // 当前层的帧指针
        Tree.Exp e = new TEMP(currentL.frame.FP());
        Level l = currentL;
        // 由于可能为外层的变量，故沿着静态链不断上溯
        while (l != acc.home) {
            e = l.staticLink().acc.exp(e);
            l = l.parent;
        }
        return new Ex(acc.acc.exp(e));
    }

    // 翻译数组元素
    public Exp transSubscriptVar(Exp var, Exp index) {
        // 数组首地址
        Tree.Exp arr_addr = var.unEx();
        // 由下标得到偏移量
        Tree.Exp arr_offset = new BINOP(BINOP.MUL, index.unEx(), new CONST(Library.WORDSIZE));
        // 产生指令，使首地址加上偏移量为数组元素实际地址
        return new Ex(new MEM(new BINOP(BINOP.PLUS, arr_addr, arr_offset)));
    }

    // 翻译域的成员变量
    public Exp transFieldVar(Exp var, int fig) {
        // 首地址
        Tree.Exp rec_addr = var.unEx();
        // 偏移量，每个成员占一个 mips 机器字
        Tree.Exp rec_offset = new CONST(fig * Library.WORDSIZE);

        return new Ex(new MEM(new BINOP(BINOP.PLUS, rec_addr, rec_offset)));
    }

    // 连接两个表达式，连接后生成无返回值的表达式
    public Exp stmcat(Exp e1, Exp e2) {
        if (e1 == null) {
            if (e2 != null) return new Nx(e2.unNx());
            else return transNoExp();
        } else if (e2 == null) {
            return new Nx(e1.unNx());
        } else {
            return new Nx(new SEQ(e1.unNx(), e2.unNx()));
        }
    }

    // 连接两个表达式，连接后生成有返回值的表达式
    public Exp exprcat(Exp e1, Exp e2) {
        if (e1 == null) {
            return new Ex(e2.unEx());
        } else {
            return new Ex(new ESEQ(e1.unNx(), e2.unEx()));
        }
    }
}
