val Max : Int = 10;

def sqr(x: Int) : Int = x * x;

def all(n: Int) : Void = {
  if n <= Max
  then print_int(sqr(n)) ; all(n + 1)
  else skip()
};

all(0)
 