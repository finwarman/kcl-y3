// Mandelbrot program

val Ymin: Double = -1.3;
val Ymax: Double =  1.3;
val Ystep: Double = 0.05;  //0.025;

val Xmin: Double = -2.1;
val Xmax: Double =  1.1;
val Xstep: Double = 0.02;  //0.01;

val Maxiters: Int = 1000;

def m_iter(m: Int, x: Double, y: Double,
                   zr: Double, zi: Double) : Void = {
  if Maxiters <= m
  then print_star() 
  else {
    if 4.0 <= zi*zi+zr*zr then print_space() 
    else m_iter(m + 1, x, y, x+zr*zr-zi*zi, 2.0*zr*zi+y) 
  }
};

def x_iter(x: Double, y: Double) : Void = {
  if x <= Xmax
  then { m_iter(0, x, y, 0.0, 0.0) ; x_iter(x + Xstep, y) }
  else skip()
};

def y_iter(y: Double) : Void = {
  if y <= Ymax
  then { x_iter(Xmin, y) ; new_line() ; y_iter(y + Ystep) }
  else skip() 
};    


y_iter(Ymin)