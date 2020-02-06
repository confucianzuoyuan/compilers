package Tree;

// CALL(f,l) 过程调用：以参数列表l调用函数f。子表达式f的计算先于参数的计算，
// 参数的计算则从左到右。
public class CALL extends Exp {
    public Exp func;
    public ExpList args;

    public CALL(Exp f, ExpList a) {
        super(null);
        func = f;
        args = a;
    }

    public ExpList kids() {
        return new ExpList(func, args);
    }

    public Exp build(ExpList kids) {
        return new CALL(kids.head, kids.tail);
    }
}
