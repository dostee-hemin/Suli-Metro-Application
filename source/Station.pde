class Station {
  String name;
  ArrayList<Station> connectingStations = new ArrayList<Station>();
  PVector position;

  Station(String name, float xPosition, float yPosition) {
    this.name = name;
    position = new PVector(xPosition, yPosition);
  }

  void addConnection(Station s) {
    connectingStations.add(s);
  }

  void display() {
    strokeWeight(15);
    stroke(50);
    point(position.x, position.y);
    fill(0);
    textSize(20);
    textAlign(CENTER);
    text(name, position.x, position.y - 20);
  }
}
