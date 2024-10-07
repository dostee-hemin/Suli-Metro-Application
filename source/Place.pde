import java.util.HashMap;

HashMap<String, Integer> tagToIndex = new HashMap<String, Integer>();

class Place {
  String name;
  String tag;
  color tagColor;
  PVector location;
  Place(String name, String tag, float x, float y) {
    this.name = name;
    this.tag = tag;
    location = new PVector(x, y);
  }

  void display(float x, float y) {
    imageMode(CENTER);
    image(placeIcons[tagToIndex.get(tag)], x,y, 44, 44);
  }
}
