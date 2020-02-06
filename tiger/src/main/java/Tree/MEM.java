package Tree;

// MEM(e) 开始于存储器地址e的wordSize个字节的内容(wordSize是在Frame模块中定义的)。
// 注意，当MEM作为MOVE操作的左子式时，它表示对存储器地址e的"存储"；在其他位置统统表示"读取"。
public class MEM extends Exp {
    public Exp exp;

    public MEM(Exp e) {
        super(null);
        exp = e;
    }

    public ExpList kids() {
        return new ExpList(exp, null);
    }

    public Exp build(ExpList kids) {
        return new MEM(kids.head);
    }
}
