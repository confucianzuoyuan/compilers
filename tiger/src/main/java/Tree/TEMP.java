package Tree;

// TEMP(t) 临时变量t，抽象机器中的临时变量，类似于真实机器中的寄存器
// 但抽象机器中可以有无限多个临时变量
public class TEMP extends Exp {
    public Temp.Temp temp;

    public TEMP(Temp.Temp t) {
        super(null);
        temp = t;
    }

    public ExpList kids() {
        return null;
    }

    public Exp build(ExpList kids) {
        return this;
    }
}
