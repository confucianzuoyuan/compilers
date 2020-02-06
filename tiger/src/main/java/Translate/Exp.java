package Translate;

public abstract class Exp {
    // 抽象类，总括其他表达式类的实现
    abstract Tree.Exp unEx(); // 转换为树表达式Tree.Exp，可以计算树的值

    abstract Tree.Stm unNx(); // 转换为树语句Tree.Stm，可以求值，但没有返回值

    // 转换为语句statement，对Tree.Stm求值，如果值为0，跳转到f，否则跳转到t
    abstract Tree.Stm unCx(Temp.Label t, Temp.Label f);
}
