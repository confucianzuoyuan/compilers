package Tree;

// CONST(t) 整型常数i
public class CONST extends Exp {
    public int value;

    public CONST(int v) {
        super(null);
        value = v;
    }

    public ExpList kids() {
        return null;
    }

    public Exp build(ExpList kids) {
        return this;
    }
}
