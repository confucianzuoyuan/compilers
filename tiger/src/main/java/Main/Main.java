package Main;

import java.io.*;
import Absyn.*;
import Frame.Frame;
import MIPS.MipsFrame;
import Semant.Semant;
import Tiger.errormsg.ErrorMsg;
import Tiger.parse.*;
import Translate.Translate;

public class Main {
    public static void main(String[] args) throws java.io.IOException {
//        String filename = "Testcases/queens.tig";
        String filename = "Testcases/Official/Bad/test14.tig";
        ErrorMsg errorMsg = new ErrorMsg(filename);
        InputStream inp = new FileInputStream(filename);
        InputStream inp2 = new FileInputStream(filename);

        PrintStream out = new PrintStream(new FileOutputStream(filename.substring(0, filename.length() - 4) + ".s"));

        Yylex lexer = new Yylex(inp, errorMsg);
        java_cup.runtime.Symbol tok;
        Yylex lexer2 = new Yylex(inp2, errorMsg);
        System.out.println("# 词法分析：");
        try {
            do {
                tok = lexer2.next_token();
                System.out.println(symnames[tok.sym] + " " + tok.left);
            } while (tok.sym != sym.EOF);
            inp2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 以上为词法分析
        System.out.println("\n# 语法分析：");
        Exp result = null;
        parser p = new parser(lexer);
        // 语法分析
        // 打印抽象语法树
        try {
            result = (Exp) p.parse().value;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Print pr = new Print(System.out);
        pr.prExp((Exp) result, 0);
        System.out.println("\n");

        Frame frame = new MipsFrame();
        Translate translator = new Translate(frame);
        // 语义分析
        Semant smt = new Semant(translator, errorMsg);
        Frag.Frag frags = smt.transProg(result);

        if (!ErrorMsg.anyErrors)
            System.out.println("无语义语法错误");
        else
            return;

//        out.close();
    }

    static String symnames[] = new String[100];
    static {

        symnames[sym.FUNCTION] = "FUNCTION";
        symnames[sym.EOF] = "EOF";
        symnames[sym.INT] = "INT";
        symnames[sym.GT] = "GT";
        symnames[sym.DIVIDE] = "DIVIDE";
        symnames[sym.COLON] = "COLON";
        symnames[sym.ELSE] = "ELSE";
        symnames[sym.OR] = "OR";
        symnames[sym.NIL] = "NIL";
        symnames[sym.DO] = "DO";
        symnames[sym.GE] = "GE";
        symnames[sym.error] = "error";
        symnames[sym.LT] = "LT";
        symnames[sym.OF] = "OF";
        symnames[sym.MINUS] = "MINUS";
        symnames[sym.ARRAY] = "ARRAY";
        symnames[sym.TYPE] = "TYPE";
        symnames[sym.FOR] = "FOR";
        symnames[sym.TO] = "TO";
        symnames[sym.TIMES] = "TIMES";
        symnames[sym.COMMA] = "COMMA";
        symnames[sym.LE] = "LE";
        symnames[sym.IN] = "IN";
        symnames[sym.END] = "END";
        symnames[sym.ASSIGN] = "ASSIGN";
        symnames[sym.STRING] = "STRING";
        symnames[sym.DOT] = "DOT";
        symnames[sym.LPAREN] = "LPAREN";
        symnames[sym.RPAREN] = "RPAREN";
        symnames[sym.IF] = "IF";
        symnames[sym.SEMICOLON] = "SEMICOLON";
        symnames[sym.ID] = "ID";
        symnames[sym.WHILE] = "WHILE";
        symnames[sym.LBRACK] = "LBRACK";
        symnames[sym.RBRACK] = "RBRACK";
        symnames[sym.NEQ] = "NEQ";
        symnames[sym.VAR] = "VAR";
        symnames[sym.BREAK] = "BREAK";
        symnames[sym.AND] = "AND";
        symnames[sym.PLUS] = "PLUS";
        symnames[sym.LBRACE] = "LBRACE";
        symnames[sym.RBRACE] = "RBRACE";
        symnames[sym.LET] = "LET";
        symnames[sym.THEN] = "THEN";
        symnames[sym.EQ] = "EQ";
    }
}
