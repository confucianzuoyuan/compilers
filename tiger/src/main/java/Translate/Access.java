package Translate;

// 用来表示变量的存储空间
public class Access {
    public Level home; // 变量所属的层
    public Frame.Access acc; // 封装Frame中的Access类

    Access(Level l, Frame.Access a) {
        home = l;
        acc = a;
    }
}
