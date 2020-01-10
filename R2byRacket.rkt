#lang racket

;;; 参考了王垠的博客：https://www.yinwang.org/blog-cn/2012/08/01/interpreter

(define env0 '())

(define ext-env
  (lambda (x v env)
    (cons `(,x . ,v) env)))

(define lookup
  (lambda (x env)
    (let ([p (assq x env)]) ; (assq x env)在env中查找包含x的元祖
      (cond
        [(not p) #f]
        [else (cdr p)]))))

(struct Closure (f env)) ; racket的struct结构

(define interp
  (lambda (exp env)
    (match exp
      [(? symbol? x)
       (let ([v (lookup x env)])
         (cond
           [(not v)
            (error "undefined variable" x)]
           [else v]))]
      [(? number? x) x]
      [`(lambda (,x) ,e)
       (Closure exp env)] ; 注意这里的 exp 就是 ``(lambda (,x) ,e)` 自己。
      [`(let ([,x ,e1]) ,e2)
       (let ([v1 (interp e1 env)])
         (interp e2 (ext-env x v1 env)))]
      [`(,e1 ,e2)
       (let ([v1 (interp e1 env)]
             [v2 (interp e2 env)])
         (match v1
           [(Closure `(lambda (,x) ,e) env-save)     ; 用模式匹配的方式取出闭包里的各个子结构
            (interp e (ext-env x v2 env-save))]))]   ; 在闭包的环境env-save中把x绑定到v2,解释函数体
      [`(,op ,e1 ,e2)
       (let ([v1 (interp e1 env)]
             [v2 (interp e2 env)])
         (match op
           ['+ (+ v1 v2)]
           ['- (- v1 v2)]
           ['* (* v1 v2)]
           ['/ (/ v1 v2)]))])))

(define r2
  (lambda (exp)
    (interp exp env0)))

(r2
'(let ([x 2])
   (let ([f (lambda (y) (* x y))])
     (f 3))))