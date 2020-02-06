package Frame;

//fp为帧指针，sp为栈指针
public abstract class Access {
    public abstract Tree.Exp exp(Tree.Exp framePtr); // 以fp为起始地址返回变量

    public abstract Tree.Exp expFromStack(Tree.Exp stackPtr); // 以sp为起始地址返回变量
}
