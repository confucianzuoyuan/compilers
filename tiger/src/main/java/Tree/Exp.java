package Tree;

// EXP(e) 计算e但忽略结果
public class Exp extends Stm {
    public Exp exp;

    public Exp(Exp e) {
        exp = e;
    }

    public ExpList kids() {
        return new ExpList(exp, null);
    }

    public Stm build(ExpList kids) {
        return new Exp(kids.head);
    }
}
