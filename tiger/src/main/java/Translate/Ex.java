package Translate;

import Temp.Label;

public class Ex extends Exp {
    Tree.Exp exp;

    public Ex(Tree.Exp e) {
        exp = e;
    }

    // Tree.Exp 本身就是有返回值表达式,无需转换直接返回
    Tree.Exp unEx() {
        return exp;
    }

    // Tree.Exp 无返回值表达式
    Tree.Stm unNx() {
        return new Tree.Exp(exp);
    }

    // 若表达式非 0 转到 t,否则转到 f
    // 若exp != 0 转到t，否则转到f
    // NE为不等于
    Tree.Stm unCx(Label t, Label f) {
        return new Tree.CJUMP(Tree.CJUMP.NE, exp, new Tree.CONST(0), t, f);
    }
}
