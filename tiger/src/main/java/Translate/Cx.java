package Translate;

import Temp.*;
import Tree.*;

public abstract class Cx extends Exp {
    Tree.Exp unEx() {
        Temp r = new Temp(); // 返回值, Temp是临时变量
        Label t = new Label(); // 真出口
        Label f = new Label(); // 假出口

        // 翻译为形如下面的程序段:
        // if (exp!=0) goto T else goto F (这句的具体处理由子类完成)
        // LABEL f:
        // r=0
        // LABEL t:
        // return r
        return new ESEQ(
                new SEQ(new MOVE(new TEMP(r), new CONST(1)), // 将常量 1 存入临时变量 r 中
                        new SEQ(unCx(t, f), // 由子类完成
                                new SEQ(new LABEL(f),
                                        // 将 0 存入临时变量 r 中
                                        new SEQ(new MOVE(new TEMP(r), new CONST(0)),
                                                new LABEL(t))))),
                new TEMP(r));
    }

    abstract Stm unCx(Label t, Label f); // 留给子类具体处理

    // 留给子类具体处理
    Stm unNx() {
        return new Tree.Exp(unEx());
    }
}
