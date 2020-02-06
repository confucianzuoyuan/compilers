package Assem;

public class InstrList {
    public Instr head;
    public InstrList tail;

    // 链表
    public InstrList(Instr h,InstrList t) {
        head = h;
        tail = t;
    }

    // 将两条指令链表链接在一起，i2 在 i1 后面
    public static InstrList append(InstrList i1, InstrList i2) {
        InstrList t = null;
        // 先将 i1 反转
        for (InstrList t1 = i1; t1 != null; t1 = t1.tail) {
            t = new InstrList(t1.head, t);
        }
        InstrList ret = i2;
        // 再将 i1 逐个接到 i2 的链表头上
        for (InstrList t2 = t; t2 != null; t2 = t2.tail) {
            ret = new InstrList(t2.head, ret);
        }
        return ret;
    }
}
