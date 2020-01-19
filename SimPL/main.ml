open Ast

let parse (s : string) : expr =
  let lexbuf = Lexing.from_string s in
  let ast = Parser.prog Lexer.read lexbuf in
  ast

type typ =
  | TInt
  | TBool

let unbound_var_err = "Unbound variable"

let bop_err = "Operator and operand type mismatch"

let if_branch_err = "Branches of if must have same type"

let if_guard_err = "Guard of if must have type bool"

(** 上下文和环境、符号表同义 *)
module type Context = sig
  type t
  val empty : t
  val lookup : t -> string -> typ
  val extend : t -> string -> typ -> t
end

module Context : Context = struct
  type t = (string * typ) list
  let empty = []
  let lookup ctx x =
    try List.assoc x ctx
    with Not_found -> failwith unbound_var_err
  let extend ctx x ty =
    (x, ty) :: ctx
end

open Context

let rec typeof ctx = function
  | Int _ -> TInt
  | Bool _ -> TBool
  | Var x -> lookup ctx x
  | Let (x, e1, e2) -> typeof_let ctx x e1 e2
  | Binop (bop, e1, e2) -> typeof_bop ctx bop e1 e2
  | If (e1, e2, e3) -> typeof_if ctx e1 e2 e3

and typeof_let ctx x e1 e2 =
  let t1 = typeof ctx e1 in
  let ctx' = extend ctx x t1 in
  typeof ctx' e2

and typeof_bop ctx bop e1 e2 =
  let t1, t2 = typeof ctx e1, typeof ctx e2 in
  match bop, t1, t2 with
  | Add, TInt, TInt
  | Mult, TInt, TInt -> TInt
  | Leq, TInt, TInt -> TBool
  | _ -> failwith bop_err

and typeof_if ctx e1 e2 e3 =
  if typeof ctx e1 = TBool
  then begin
    let t2 = typeof ctx e2 in
    if t2 = typeof ctx e3 then t2
    else failwith if_branch_err
  end
  else failwith if_guard_err

let typecheck e =
  ignore (typeof empty e)

let is_value : expr -> bool = function
  | Int _ | Bool _ -> true
  | Var _ | Let _ | Binop _ | If _ -> false

let rec subst e v x = match e with
  | Var y -> if x = y then v else e
  | Bool _ -> e
  | Int _ -> e
  | Binop (bop, e1, e2) -> Binop (bop, subst e1 v x, subst e2 v x)
  | Let (y, e1, e2) ->
    let e1' = subst e1 v x in
    if x = y
    then Let (y, e1', e2)
    else Let (y, e1', subst e2 v x)
  | If (e1, e2, e3) ->
    If (subst e1 v x, subst e2 v x, subst e3 v x)

let rec step : expr -> expr = function
  | Int _ | Bool _ -> failwith "Does not step"
  | Var _ -> failwith unbound_var_err
  | Binop (bop, e1, e2) when is_value e1 && is_value e2 ->
    step_bop bop e1 e2
  | Binop (bop, e1, e2) when is_value e1 ->
    Binop (bop, e1, step e2)
  | Binop (bop, e1, e2) -> Binop (bop, step e1, e2)
  | Let (x, e1, e2) when is_value e1 -> subst e2 e1 x
  | Let (x, e1, e2) -> Let (x, step e1, e2)
  | If (Bool true, e2, _) -> e2
  | If (Bool false, _, e3) -> e3
  | If (Int _, _, _) -> failwith if_guard_err
  | If (e1, e2, e3) -> If (step e1, e2, e3)

and step_bop bop e1 e2 = match bop, e1, e2 with
  | Add, Int a, Int b -> Int (a + b)
  | Mult, Int a, Int b -> Int (a * b)
  | Leq, Int a, Int b -> Bool (a <= b)
  | _ -> failwith bop_err

let rec eval_small (e : expr) : expr =
  if is_value e then e
  else e |> step |> eval_small

let interp_small (s : string) : expr =
  let e = parse s in
  typecheck e;
  eval_small e

let rec eval_big (e : expr) : expr = match e with
  | Int _ | Bool _ -> e
  | Var _ -> failwith unbound_var_err
  | Binop (bop, e1, e2) -> eval_bop bop e1 e2
  | Let (x, e1, e2) -> subst e2 (eval_big e1) x |> eval_big
  | If (e1, e2, e3) -> eval_if e1 e2 e3

and eval_bop bop e1 e2 = match bop, eval_big e1, eval_big e2 with
  | Add, Int a, Int b -> Int (a + b)
  | Mult, Int a, Int b -> Int (a * b)
  | Leq, Int a, Int b -> Bool (a <= b)
  | _ -> failwith bop_err

and eval_if e1 e2 e3 = match eval_big e1 with
  | Bool true -> eval_big e2
  | Bool false -> eval_big e3
  | _ -> failwith if_guard_err

let interp_big (s : string) : expr =
  let e = parse s in
  typecheck e;
  eval_big e