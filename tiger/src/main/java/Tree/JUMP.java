package Tree;

import Temp.Label;
import Temp.LabelList;

// JUMP(e,labs) 将控制转移到地址e，目标地址e可以是文字符号，例如用NAME(lab)
// 表示的符号，也可以是由其他种类的表达式计算出来的一个地址。标号表labs指出表达式e
// 可能计算出的所有目标地址；数据流分析会需要它们。
// 转移到一个已知标号l的普通情形可表示为：
// T_Jump(l, Temp_LabelList(l, NULL));
// 以上为C语言版中的虎书注释
public class JUMP extends Stm {
    public Exp exp;
    public LabelList targets;

    public JUMP(Exp e, LabelList t) {
        exp = e;
        targets = t;
    }

    public JUMP(Label target) {
        this(new NAME(target), new LabelList(target, null));
    }

    public ExpList kids() {
        return new ExpList(exp, null);
    }

    public Stm build(ExpList kids) {
        return new JUMP(kids.head, targets);
    }
}
