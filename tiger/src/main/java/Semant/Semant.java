package Semant;

import Absyn.FieldList;
import Symbol.Symbol;
import Tiger.errormsg.ErrorMsg;
import Translate.Level;
import Types.*;
import Util.BoolList;

public class Semant {
    private Env env; // 符号表
    private Translate.Translate trans;
    private Translate.Level level = null;
    // 用于循环嵌套的堆栈
    private java.util.Stack<Temp.Label> loopStack = new java.util.Stack<Temp.Label>();
    private Boolean TDecFlag = false, FDecFlag = false, TypeDecFlag = false, FuncDecFlag = false;

    public Semant(Translate.Translate t, ErrorMsg err) {
        trans = t;
        level = new Level(t.frame);
        // 初始化，加入 main 函数的层
        level = new Level(level, Symbol.symbol("main"), null);
        // 初始化符号表
        env = new Env(err, level);
    }

    public Frag.Frag transProg(Absyn.Exp e) {
        // 进行语义检查并翻译为 IR 树
        ExpTy et = transExp(e);
        // 若检查到语义错误则停止编译
        if (ErrorMsg.anyErrors) {
            System.err.println("检测到语义错误，编译终止");
            return null;
        }
        // 添加函数调用部分代码
        trans.procEntryExit(level, et.exp, false);
        // 回到上一层
        level = level.parent;
        // 返回翻译完的段列表
        return trans.getResult();
    }

    public ExpTy transVar(Absyn.Var e) {
        // 翻译变量调用，分别调用普通简单变量、数组变量和域变量的重载函数
        if (e instanceof Absyn.SimpleVar) return transVar((Absyn.SimpleVar)e);
        if (e instanceof Absyn.SubscriptVar) return transVar((Absyn.SubscriptVar)e);
        if (e instanceof Absyn.FieldVar) return transVar((Absyn.FieldVar)e);
        return null;
    }

    public ExpTy transExp(Absyn.Exp e) {
        // 调用重载函数翻译表达式
        if (e instanceof Absyn.IntExp) return transExp((Absyn.IntExp)e);
        if (e instanceof Absyn.StringExp) return transExp((Absyn.StringExp)e);
        if (e instanceof Absyn.NilExp) return transExp((Absyn.NilExp)e);
        if (e instanceof Absyn.VarExp) return transExp((Absyn.VarExp)e);
        if (e instanceof Absyn.OpExp) return transExp((Absyn.OpExp)e);
        if (e instanceof Absyn.AssignExp) return transExp((Absyn.AssignExp)e);
        if (e instanceof Absyn.CallExp) return transExp((Absyn.CallExp)e);
        if (e instanceof Absyn.RecordExp) return transExp((Absyn.RecordExp)e);
        if (e instanceof Absyn.ArrayExp) return transExp((Absyn.ArrayExp)e);
        if (e instanceof Absyn.IfExp) return transExp((Absyn.IfExp)e);
        if (e instanceof Absyn.WhileExp) return transExp((Absyn.WhileExp)e);
        if (e instanceof Absyn.ForExp) return transExp((Absyn.ForExp)e);
        if (e instanceof Absyn.BreakExp) return transExp((Absyn.BreakExp)e);
        if (e instanceof Absyn.LetExp) return transExp((Absyn.LetExp)e);
        if (e instanceof Absyn.SeqExp) return transExp((Absyn.SeqExp)e);
        return null;
    }

    private ExpTy transExp(Absyn.IntExp e) {
        // 翻译整型表达式
        return new ExpTy(trans.transIntExp(e.value), new INT());
    }

    private ExpTy transExp(Absyn.StringExp e) {
        // 翻译字符串表达式
        return new ExpTy(trans.transStringExp(e.value), new STRING());
    }

    private ExpTy transExp(Absyn.NilExp e) {
        // 翻译空表达式
        return new ExpTy(trans.transNilExp(), new NIL());
    }

    private ExpTy transExp(Absyn.VarExp e) {
        // 翻译变量表达式
        return transVar(e.var);
    }

    private ExpTy transExp(Absyn.OpExp e) {
        // 翻译 binary 表达式
        ExpTy el = transExp(e.left); // 翻译左子树，即第一个操作数
        ExpTy er = transExp(e.right); // 翻译右子树，即第二个操作数
        if (el == null || er == null) return null;
        // 语义检查
        // 检查相等或者不相等运算
        if (e.oper == Absyn.OpExp.EQ || e.oper == Absyn.OpExp.NE) {
            // 若比较运算符的两边都是 nil ，则报错
            if (el.ty.actual() instanceof NIL && er.ty.actual() instanceof NIL) {
                env.errorMsg.error(e.pos, " Nil类型不能域Nil类型比较");
                return null;
            }
            // void 类型不能参与比较
            if (el.ty.actual() instanceof VOID || er.ty.actual() instanceof VOID) {
                env.errorMsg.error(e.pos, "不允许Void类型参与比较");
                return null;
            }
            // 若一方为 nil 而另一方为域类型，则照常翻译为二目运算语句
            if (el.ty.actual() instanceof NIL && er.ty.actual() instanceof RECORD) {
                return new ExpTy(trans.transOpExp(e.oper, transExp(e.left).exp, transExp(e.right).exp), new INT());
            }
            if (el.ty.actual() instanceof RECORD && er.ty.actual() instanceof NIL) {
                return new ExpTy(trans.transOpExp(e.oper, transExp(e.left).exp, transExp(e.right).exp), new INT());
            }
            // 此外，若两边类型一致
            if (el.ty.coerceTo(er.ty)) {
                // 若为字符串类型，则需要调用 Translate 中的 transStringRelExp 特别进行翻译，因为字符串类型的比较域普通类型不同，需要调用库函数
                if (el.ty.actual() instanceof STRING && e.oper == Absyn.OpExp.EQ) {
                    return new ExpTy(trans.transStringRelExp(level, e.oper, transExp(e.left).exp, transExp(e.right).exp), new INT());
                }
                // 否则翻译为普通二目运算语句
                return new ExpTy(trans.transOpExp(e.oper, transExp(e.left).exp, transExp(e.right).exp), new INT());
            }
            // 否则两边类型不一致，报错
            env.errorMsg.error(e.pos, "运算符两边类型不一致");
            return null;
        }
        // 若运算符为< <= > >= ，则翻译为普通二目运算语句，注意区分整数和字符串的情况
        if (e.oper > Absyn.OpExp.NE) {
            if (el.ty.actual() instanceof INT && er.ty.actual() instanceof INT) {
                return new ExpTy(trans.transOpExp(e.oper, transExp(e.left).exp, transExp(e.right).exp), new INT());
            }
            if (el.ty.actual() instanceof STRING && er.ty.actual() instanceof STRING) {
                return new ExpTy(trans.transOpExp(e.oper, transExp(e.left).exp, transExp(e.right).exp), new STRING());
            }
            // 否则两边类型不一致或类型不支持比较运算
            env.errorMsg.error(e.pos, "两边类型不一致或此类型不支持比较运算");
            return null;
        }
        // 若为加减乘除算数运算
        if (e.oper < Absyn.OpExp.EQ) {
            // 若两边类型一致则翻译为普通二目运算语句
            if (el.ty.actual() instanceof INT && er.ty.actual() instanceof INT) {
                return new ExpTy(trans.transOpExp(e.oper, transExp(e.left).exp, transExp(e.right).exp), new INT());
            }
            // 若两边类型不一致，则报错
            env.errorMsg.error(e.pos, "运算符两边类型不一致");
            return null;
        }

        return new ExpTy(trans.transOpExp(e.oper, el.exp, er.exp), new INT());
    }

    // 翻译赋值表达式
    private ExpTy transExp(Absyn.AssignExp e) {
        int pos = e.pos;
        Absyn.Var var = e.var; // 左值
        Absyn.Exp exp = e.exp; // 右值
        ExpTy er = transExp(exp);
        // 若右值表达式类型为void，即无返回值，则报错
        if (er.ty.actual() instanceof VOID) {
            env.errorMsg.error(pos, "不能为变量赋控制");
            return null;
        }
        // 若左值为简单变量
        if (var instanceof Absyn.SimpleVar) {
            Absyn.SimpleVar ev = (Absyn.SimpleVar)var;
            // 查找符号表，得到变量入口
            Entry x = (Entry)(env.vEnv.get(ev.name));
            // 若为循环变量则报错
            if (x instanceof VarEntry && ((VarEntry)x).isFor) {
                env.errorMsg.error(pos, "循环变量不能被赋值");
                return null;
            }
        }
        // 若不是简单变量，就把这个简单变量翻译了
        ExpTy vr = transVar(var);
        // 若赋值运算两边类型不匹配则报错
        if (!er.ty.coerceTo(vr.ty)) {
            env.errorMsg.error(pos, er.ty.actual().getClass().getSimpleName() + "类型的值不能赋值给" + vr.ty.actual().getClass().getSimpleName() + "类型的变量");
            return null;
        }
        // 若通过检查则翻译为赋值表达式，注意赋值表达式返回值为void，即没有返回值
        return new ExpTy(trans.transAssignExp(vr.exp, er.exp), new VOID());
    }

    // 翻译函数调用表达式
    private ExpTy transExp(Absyn.CallExp e) {
        FuncEntry fe;
        Object x = env.vEnv.get(e.func);
        // 检查符号表，若符号表中查找不到则报错
        if (x == null || !(x instanceof FuncEntry)) {
            env.errorMsg.error(e.pos, "函数" + e.func.toString() + "未定义");
            return null;
        }
        // 参数表，此为实参
        Absyn.ExpList ex = e.args;
        // 函数入口
        fe = (FuncEntry)x;
        // 参数表，此为形式参数
        RECORD rc = fe.paramlist;
        // 检查形式参数与实际参数是否一致
        while (ex != null) {
            if (rc == null) {
                env.errorMsg.error(e.pos, "参数表不一致，调用函数时传入了多余参数");
                return null;
            }
            if (!transExp(ex.head).ty.coerceTo(rc.fieldType)) {
                env.errorMsg.error(e.pos, "参数表类型不一致");
                return null;
            }
            ex = ex.tail;
            rc = rc.tail;
        }
        if (ex == null && !(RECORD.isNull(rc))) {
            env.errorMsg.error(e.pos, "参数表不一致，缺少参数传入");
            return null;
        }
        // 逐个翻译每个实参所对应的表达式
        java.util.ArrayList<Translate.Exp> arrl = new java.util.ArrayList<>();
        for (Absyn.ExpList i = e.args; i != null; i = i.tail) {
            arrl.add(transExp(i.head).exp);
        }
        // 若为标准库函数，则调用transStdCallExp特别处理
        if (x instanceof StdFuncEntry) {
            StdFuncEntry sf = (StdFuncEntry)x;
            return new ExpTy(trans.transStdCallExp(level, sf.label, arrl), sf.returnTy);
        }
        // 否则作为普通函数处理，二者的区别在于标准库不必处理静态链，故不必传入函数的层
        return new ExpTy(trans.transCallExp(level, fe.level, fe.label, arrl), fe.returnTy);
    }

    // 翻译记录的定义表达式
    private ExpTy transExp(Absyn.RecordExp e) {
        // 查找类型符号表，找不到则报错
        Type t = (Type)env.tEnv.get(e.typ);
        if (t == null || !(t.actual() instanceof RECORD)) {
            env.errorMsg.error(e.pos, "此记录类型不存在");
            return null;
        }
        // 比较类型中的每个成员与此表达式是否一致，不一致则报错
        Absyn.FieldExpList fe = e.fields;
        RECORD rc = (RECORD)(t.actual());
        if (fe == null && rc != null) {
            env.errorMsg.error(e.pos, "记录类型中的成员变量不一致");
            return null;
        }

        while (fe != null) {
            ExpTy ie = transExp(fe.init);
            if (rc == null || ie == null || !ie.ty.coerceTo(rc.fieldType) || fe.name != rc.fieldName) {
                env.errorMsg.error(e.pos, "记录类型中的成员变量不一致");
                return null;
            }
            fe = fe.tail;
            rc = rc.tail;
        }

        // 将记录中每个成员变量所对应的表达式装入一个链表中，并作为参数传入
        java.util.ArrayList<Translate.Exp> arrl = new java.util.ArrayList<>();
        for (Absyn.FieldExpList i = e.fields; i != null; i = i.tail) {
            arrl.add(transExp(i.init).exp);
        }
        return new ExpTy(trans.transRecordExp(level, arrl), t.actual());
    }

    private ExpTy transExp(Absyn.ArrayExp e) {
        // 翻译数组定义表达式
        Type ty = (Type)env.tEnv.get(e.typ);
        // 在类型符号表中查找数组类型，找不到则报错
        if (ty == null || !(ty.actual() instanceof ARRAY)) {
            env.errorMsg.error(e.pos, "此数组不存在");
            return null;
        }
        // 翻译数组的下标
        ExpTy size = transExp(e.size);
        // 下标必须为整型，不然则报错
        if (!(size.ty.actual() instanceof INT)) {
            env.errorMsg.error(e.pos, "数组的长度不是整型");
            return null;
        }
        // 翻译数组初始值的表达式，若初始值的类型与数组元素的类型不一致，则报错
        ARRAY ar = (ARRAY)ty.actual();
        ExpTy ini = transExp(e.init);
        if (!ini.ty.coerceTo(ar.element.actual())) {
            env.errorMsg.error(e.pos, "初始值的类型与数组元素的类型不一致");
            return null;
        }
        return new ExpTy(trans.transArrayExp(level, ini.exp, size.exp), new ARRAY(ar.element));
    }

    // 翻译 if 语句
    private ExpTy transExp(Absyn.IfExp e) {
        ExpTy testET = transExp(e.test); // 翻译控制条件
        ExpTy thenET = transExp(e.thenclause); // 翻译条件为真时运行的程序
        ExpTy elseET = transExp(e.elseclause); // 翻译条件为假时运行的程序
        // 控制条件必须为 int 类型的表达式，不然则报错
        if (e.test == null || testET == null || !(testET.ty.actual() instanceof INT)) {
            env.errorMsg.error(e.pos, "if语句中的条件表达式不是整型");
            return null;
        }
        // 若没有 false 分支，则 if 语句不应该有返回值
        if (e.elseclause == null && (!(thenET.ty.actual() instanceof VOID))) {
            env.errorMsg.error(e.pos, "不应该有返回值");
            return null;
        }
        // 如果真、假分支都存在，则二者表达式的类型应该一致
        if (e.elseclause != null && !thenET.ty.coerceTo(elseET.ty)) {
            env.errorMsg.error(e.pos, "两个分支的类型不一致");
            return null;
        }
        // 如果没有假分支，则将假分支作为空语句翻译
        if (elseET == null) {
            return new ExpTy(trans.transIfExp(testET.exp, thenET.exp, trans.transNoExp()), thenET.ty);
        }
        return new ExpTy(trans.transIfExp(testET.exp, thenET.exp, elseET.exp), thenET.ty);
    }

    // 翻译 break 语句
    // 若 break 语句不在循环内使用则报错
    private ExpTy transExp(Absyn.BreakExp e) {
        if (loopStack.isEmpty()) {
            env.errorMsg.error(e.pos, "break语句不在循环内");
            return null;
        }
        // 传入当前的循环
        return new ExpTy(trans.transBreakExp(loopStack.peek()), new VOID());
    }

    // 翻译 let-in-end 语句
    private ExpTy transExp(Absyn.LetExp e) {
        Translate.Exp ex = null;
        // let ~ in 之间新开一个作用域
        env.vEnv.beginScope();
        env.tEnv.beginScope();
        // dec: 声明语句
        // 翻译类型、变量、函数申明语句
        ExpTy td = transDecList(e.decs);
        if (td != null) ex = td.exp;
        // 翻译 in ~ end 之间的程序
        ExpTy tb = transExp(e.body);
        if (tb == null) ex = trans.stmcat(ex, null);
        else if (tb.ty.actual() instanceof VOID) ex = trans.stmcat(ex, tb.exp);
        else ex = trans.exprcat(ex, tb.exp); // // 将两部分连接在一起

        // 结束作用域
        env.tEnv.endScope();
        env.vEnv.endScope();

        return new ExpTy(ex, tb.ty);

    }

    // 翻译声明列表
    private ExpTy transDecList(Absyn.DecList e) {
        Translate.Exp ex = null;
        // 声明的翻译要进行两轮，第一轮中要将所有符号放入符号表
        for (Absyn.DecList i = e; i != null; i = i.tail) transDec0(i.head);
        for (Absyn.DecList i = e; i != null; i = i.tail) ex = trans.stmcat(ex, transDec(i.head));

        return new ExpTy(ex, new VOID());
    }

    // 调用重载函数翻译变量、类型、函数定义
    public Translate.Exp transDec(Absyn.Dec e) {
        if (e instanceof Absyn.VarDec) {
            if (TypeDecFlag) {
                TDecFlag = true;
            }
            if (FuncDecFlag) {
                FDecFlag = true;
            }
            return transDec((Absyn.VarDec)e);
        }
        if (e instanceof Absyn.TypeDec) {
            if (!TypeDecFlag) {
                TypeDecFlag = true;
                return transDec((Absyn.TypeDec)e);
            }
            if (TDecFlag) {
                env.errorMsg.error(e.pos, "类型定义被中途打断");
                return null;
            }
        }
        if (e instanceof Absyn.FunctionDec) {
            if (!FuncDecFlag) {
                FuncDecFlag = true;
                return transDec((Absyn.FunctionDec)e);
            }
            if (FDecFlag) {
                env.errorMsg.error(e.pos, "函数定义被中途打断");
            }
        }
        return null;
    }

    // 调用重载函数翻译变量、类型、函数定义
    public void transDec0(Absyn.Dec e) {
        if (e instanceof Absyn.VarDec) transDec0((Absyn.VarDec)e);
        if (e instanceof Absyn.TypeDec) transDec0((Absyn.TypeDec)e);
        if (e instanceof Absyn.FunctionDec) transDec0((Absyn.FunctionDec)e);
    }

    private void transDec0(Absyn.VarDec e) {}

    private void transDec0(Absyn.TypeDec e) {
        // 采用哈希表注意检查是否有重复定义，注意变量定义若有重复则直接覆盖，而类型定义若重复则报错
        java.util.HashSet<Symbol> hs = new java.util.HashSet<Symbol>();
        for (Absyn.TypeDec i = e; i != null; i = i.next) {
            if (hs.contains(i.name)) {
                env.errorMsg.error(e.pos, "同一个块中类型重复定义");
                return;
            }
            hs.add(i.name);
            env.tEnv.put(i.name, new NAME(i.name));
        }
    }

    // FunctonDec 为链表，这个程序中链表用的很多
    private void transDec0(Absyn.FunctionDec e) {
        for (Absyn.FunctionDec i = e; i != null; i = i.next) {
            Absyn.RecordTy rt = new Absyn.RecordTy(i.pos, i.params);
            RECORD r = transTy(rt);
            if (r == null) return;
            // 后检查参数列表,与记录类型 RecordTy 的检查完全相同,得到 RECORD 类型的形参列表
            BoolList bl = null;
            for (FieldList f = i.params; f != null; f = f.tail) {
                bl = new BoolList(true, bl); // 构建链表的方式，这里把每一个变量都当成逃逸变量了，方便简单处理
            }
            // 这里的代码有些不明白
            level = new Level(level, i.name, bl);
            // 把函数声明放进值的符号表中
            env.vEnv.put(i.name, new FuncEntry(level, new Temp.Label(i.name), r, transTy(i.result)));
            level = level.parent;
        }
    }

    // 调用重载函数翻译变量类型
    public Type transTy(Absyn.Ty e) {
        if (e instanceof Absyn.ArrayTy) return transTy((Absyn.ArrayTy)e);
        if (e instanceof Absyn.RecordTy) return transTy((Absyn.RecordTy)e);
        if (e instanceof Absyn.NameTy) return transTy((Absyn.NameTy)e);
        return null;
    }

    // 翻译未知类型 NameTy
    private Type transTy(Absyn.NameTy e) {
        if (e == null) return new VOID();
        // 检查入口类型符号表，若找不到则报错
        Type t = (Type)env.tEnv.get(e.name);
        if (t == null) {
            env.errorMsg.error(e.pos, "类型未定义");
            return null;
        }
        return t;
    }

    private ARRAY transTy(Absyn.ArrayTy e) {
        Type t = (Type) env.tEnv.get(e.typ);
        // 检查入口符号表,若找不到则报错
        if (t == null) {
            env.errorMsg.error(e.pos, "类型不存在");
            return null;
        }
        return new ARRAY(t);
    }

    private RECORD transTy(Absyn.RecordTy e) {
        RECORD rc = new RECORD(), r = new RECORD();
        if (e == null || e.fields == null) {
            rc.gen(null, null, null);
            return rc;
        }
        // 检查该记录类型每个域的类型在 tEnv 中是否存在，若不存在，则报告未知类型错误
        Absyn.FieldList fl = e.fields;
        boolean first = true;
        while (fl != null) {
            if (env.tEnv.get(fl.typ) == null) {
                env.errorMsg.error(e.pos, "域类型不存在");
                return null;
            }

            rc.gen(fl.name, (Type)env.tEnv.get(fl.typ), new RECORD());
            if (first) {
                r = rc; // 引用？保留链表头指针？
                first = false;
            }
            if (fl.tail == null) rc.tail = null;
            rc = rc.tail;
            fl = fl.tail;
        }

        return r;
    }

    // 翻译变量定义
    private Translate.Exp transDec(Absyn.VarDec e) {
        // 翻译初始值
        ExpTy et = transExp(e.init);
        // 初始值不能为 nil
        if (e.typ == null && e.init instanceof Absyn.NilExp) {
            env.errorMsg.error(e.pos, "初始值不能赋值为nil");
            return null;
        }
        // 除了记录类型外，其他变量定义必须赋初始值
        if (et == null && e.init == null) {
            env.errorMsg.error(e.pos, "定义变量必须赋初始值");
            return null;
        }
        // 这里特别处理初始值赋值为()的情况
        if (et == null) {
            et = new ExpTy(trans.transNilExp(), new NIL());
            e.init = new Absyn.NilExp(e.pos);
        }
        // 若初始值与变量类型不匹配则报错
        if (e.typ != null && !(transExp(e.init).ty.coerceTo((Type)env.tEnv.get(e.typ.name)))) {
            env.errorMsg.error(e.pos, "初始值与变量类型不匹配");
            return null;
        }
        if (e.init == null) {
            env.errorMsg.error(e.pos, "定义变量必须赋初始值");
            return null;
        }
        // 为变量分配空间
        Translate.Access acc = level.allocLocal(true);
        // 将变量加入入口符号表，分简略声明与长声明两种，简略声明不用写明变量类型，其类型由初始值定义，简单的类型推断
        if (e.typ != null) {
            env.vEnv.put(e.name, new VarEntry((Type)env.tEnv.get(e.typ.name), acc));
        } else {
            env.vEnv.put(e.name, new VarEntry(transExp(e.init).ty, acc));
        }
        return trans.transAssignExp(trans.transSimpleVar(acc, level), et.exp);
    }

    private Translate.Exp transDec(Absyn.FunctionDec e) {
        // 翻译函数声明
        java.util.HashSet<Symbol> hs = new java.util.HashSet<Symbol>();
        ExpTy et = null;
        // 检查重复声明，分为普通函数与标准库函数
        for (Absyn.FunctionDec i = e; i != null; i = i.next) {
            if (hs.contains(i.name)) {
                env.errorMsg.error(e.pos, "在同一个块中重复定义函数");
                return null;
            }
            if (env.stdFuncSet.contains(i.name)) {
                env.errorMsg.error(e.pos, "与标准库函数重名");
                return null;
            }

            Absyn.RecordTy rt = new Absyn.RecordTy(i.pos, i.params);
            RECORD r = transTy(rt);
            if (r == null) return null;
            // 后检查参数列表，与记录类型 RecordTy 的检查完全相同，得到 RECORD 类型的形参列表
            BoolList bl = null;
            for (FieldList f = i.params; f != null; f = f.tail) {
                bl = new BoolList(true, bl);
            }
            // level这里的链表是怎么作用的？具体链表里面都有些什么东西？
            level = new Level(level, i.name, bl);
            // 新建一个词法作用域
            env.vEnv.beginScope();
            Translate.AccessList al = level.formals.next;
            for (RECORD j = r; j != null; j = j.tail) {
                if (j.fieldName != null) {
                    env.vEnv.put(j.fieldName, new VarEntry(j.fieldType, al.head));
                    al = al.next;
                }
            }
            // 翻译函数体
            et = transExp(i.body);
            if (et == null) {
                env.vEnv.endScope();
                return null;
            }
            if (!(et.ty.coerceTo((transTy(i.result).actual())))) {
                env.errorMsg.error(i.pos, "函数定义中返回值类型不匹配");
                return null;
            }
            // 接着检查函数返回值，如果没有返回值则设置成 void
            // 判断是否为 void，若不为 void 则要将返回值存入 $v0 寄存器
            if (!(et.ty.actual() instanceof VOID)) trans.procEntryExit(level, et.exp, true);
            else trans.procEntryExit(level, et.exp, false);

            env.vEnv.endScope();
            // 回到原来的层
            level = level.parent;
            hs.add(i.name);
        }
        return trans.transNoExp();
    }

    // 翻译数组变量（右值）
    // 数组下标必须为整型，否则报错
    private ExpTy transVar(Absyn.SubscriptVar e) {
        if (!(transExp(e.index).ty.actual() instanceof INT)) {
            env.errorMsg.error(e.pos, "下标必须为整数");
            return null;
        }
        // 翻译数组入口
        ExpTy ev = transVar(e.var);
        // 翻译数组下标的表达式
        ExpTy ei = transExp(e.index);
        // 若入口为空则报错
        if (ev == null || !(ev.ty.actual() instanceof ARRAY)) {
            env.errorMsg.error(e.pos, "数组不存在");
            return null;
        }
        ARRAY ae = (ARRAY)(ev.ty.actual());
        return new ExpTy(trans.transSubscriptVar(ev.exp, ei.exp), ae.element);
    }

    // 翻译域变量(右值)
    private ExpTy transVar(Absyn.FieldVar e) {
        ExpTy et = transVar(e.var);
        // 若除去域部分后不是记录类型,则报错
        if (!(et.ty.actual() instanceof RECORD)) {
            env.errorMsg.error(e.pos, "此变量不是一个记录类型");
            return null;
        }
        // 逐个查找记录的域，如果没有一个匹配当前域变量的域，则报错
        RECORD rc = (RECORD)(et.ty.actual());
        int count = 1;
        while (rc != null) {
            if (rc.fieldName == e.field) {
                return new ExpTy(trans.transFieldVar(et.exp, count), rc.fieldType);
            }
            count++;
            rc = rc.tail;
        }
        env.errorMsg.error(e.pos, "域变量不存在");
        return null;
    }

    // 翻译类型声明语句
    private Translate.Exp transDec(Absyn.TypeDec e) {
        for (Absyn.TypeDec i = e; i != null; i = i.next) {
            env.tEnv.put(i.name, new NAME(i.name));
            // 没看明白
            ((NAME)env.tEnv.get(i.name)).bind(transTy(i.ty).actual());
            NAME field = (NAME)env.tEnv.get(i.name);
            if (field.isLoop()) {
                env.errorMsg.error(i.pos, "类型循环定义");
                return null;
            }
        }
        // 将类型放入类型符号表
        for (Absyn.TypeDec i = e; i != null; i = i.next)
            env.tEnv.put(i.name, transTy(i.ty));

        return trans.transNoExp();
    }

    // 翻译表达式序列
    private ExpTy transExp(Absyn.SeqExp e) {
        Translate.Exp ex = null;
        for (Absyn.ExpList t = e.list; t != null; t = t.tail) {
            ExpTy x = transExp(t.head);

            if (t.tail == null) {
                if (x != null) {
                    if (x.ty.actual() instanceof VOID) {
                        ex = trans.stmcat(ex, x.exp);
                    } else {
                        ex = trans.exprcat(ex, x.exp);
                    }
                }
                if (x != null)
                    return new ExpTy(ex, x.ty);
                else
                    return new ExpTy(ex, new VOID());
            }
            ex = trans.stmcat(ex, x.exp);
        }
        return null;
    }

    // 翻译 while 循环语句
    private ExpTy transExp(Absyn.WhileExp e) {
        ExpTy transt = transExp(e.test); // 翻译循环条件
        if (transt == null) return null;
        // 循环条件必须为整数类型
        if (!(transt.ty.actual() instanceof INT)) {
            env.errorMsg.error(e.pos, "循环条件不是整数类型");
            return null;
        }
        // 循环出口的标记
        Temp.Label out = new Temp.Label();
        loopStack.push(out); // 将循环压栈一遍处理循环嵌套
        ExpTy bdy = transExp(e.body); // 翻译循环体
        loopStack.pop(); // 将当前循环弹出栈

        if (bdy == null)
            return null;
        // while循环无返回值
        if (!(bdy.ty.actual() instanceof VOID)) {
            env.errorMsg.error(e.pos, "while循环不能返回值");
            return null;
        }

        return new ExpTy(trans.transWhileExp(transt.exp, bdy.exp, out), new VOID());
    }

    private ExpTy transVar(Absyn.SimpleVar e) {
        // 翻译简单变量(右值)
        Entry ex = (Entry) env.vEnv.get(e.name);
        // 查找入口符号表,找不到则报错
        if (ex == null || !(ex instanceof VarEntry)) {
            env.errorMsg.error(e.pos, "变量未定义");
            return null;
        }
        VarEntry evx = (VarEntry) ex;
        return new ExpTy(trans.transSimpleVar(evx.acc, level), evx.Ty);
    }

    // 翻译 for 循环
    private ExpTy transExp(Absyn.ForExp e) {
        boolean flag = false; // 标记循环体是否为空
        // 循环变量必须是整型
        if (!(transExp(e.hi).ty.actual() instanceof INT) || !(transExp(e.var.init).ty.actual() instanceof INT)) {
            env.errorMsg.error(e.pos, "循环变量必须是整数类型");
        }
        // 由于需要为循环变量分配存储空间，故需要新开始一个作用域
        env.vEnv.beginScope();
        Temp.Label label = new Temp.Label(); // 定义循环的入口
        // 循环入栈
        loopStack.push(label);
        // 为循环变量分配空间
        Translate.Access acc = level.allocLocal(true);
        // 将循环变量加入变量符号表
        env.vEnv.put(e.var.name, new VarEntry(new INT(), acc, true));
        // 翻译循环体
        ExpTy body = transExp(e.body);
        // 翻译循环变量的最终值表达式
        ExpTy high = transExp(e.hi);
        // 翻译循环变量的初始值表达式
        ExpTy low = transExp(e.var.init);
        if (body == null) flag = true;
        // 循环弹出栈
        loopStack.pop();
        // 结束当前的作用域
        env.vEnv.endScope();

        if (flag) return null;
        return new ExpTy(trans.transForExp(level, acc, low.exp, high.exp, body.exp, label), new VOID());
    }
}
