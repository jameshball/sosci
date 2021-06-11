import java.util.Objects;

public final class Vector2 {

  private final double x;
  private final double y;

  public Vector2(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public Vector2() {
    this(0, 0);
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public Vector2 setX(double x) {
    return new Vector2(x, this.y);
  }

  public Vector2 setY(double y) {
    return new Vector2(this.x, y);
  }

  public Vector2 copy() {
    return new Vector2(x, y);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Vector2 point = (Vector2) obj;
    return round(x, 2) == round(point.x, 2)
      && round(y, 2) == round(point.y, 2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(round(x, 2), round(y, 2));
  }

  private static double round(double value, int places) {
    if (places < 0) {
      throw new IllegalArgumentException();
    }

    long factor = (long) Math.pow(10, places);
    value *= factor;

    return (double) Math.round(value) / factor;
  }

  @Override
  public String toString() {
    return "Vector2{" +
      "x=" + x +
      ", y=" + y +
      '}';
  }
}
