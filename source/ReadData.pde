PImage displayMap;
PImage referenceDisplay;
PImage backgroundDisplay;
PImage pinIcon;
PImage resetIcon;
PImage[] placeIcons = new PImage[9];
PImage[] directionIcons = new PImage[2];

void loadImages() {
  displayMap = loadImage("Data/Images/display.png");
  referenceDisplay = loadImage("Data/Images/old display.png");
  backgroundDisplay = loadImage("Data/Images/background display.png");
  directionIcons[0] = loadImage("Data/Images/direction 0.png");
  directionIcons[1] = loadImage("Data/Images/direction 1.png");
  resetIcon = loadImage("Data/Images/reset.png");
}

void loadMetroRoute() {
  String[] txt = loadStrings("Data/Saved Info/route details.txt");
  int numStations = int(txt[0]);
  for (int i=0; i<numStations; i++) {
    String name = txt[i*2+1];
    String[] coordinates = txt[i*2+2].split(" ");
    stations.add(new Station(name, int(coordinates[0]), int(coordinates[1])));
  }
  for (int i=0; i<numStations; i++) {
    Station s = stations.get(i);
    stationNodes.add(new Node(s));
    String[] connections = txt[1+numStations*2+i].split(",");
    for (String otherName : connections) {
      for (Station other : stations) {
        if (other == s) continue;

        if (other.name.equals(otherName)) s.addConnection(other);
      }
    }
  }

  int currentIndex = 1+numStations*3;
  int numRoutes = int(txt[currentIndex++]);
  for (int i=0; i<numRoutes; i++) {
    String[] colorValues = txt[currentIndex++].split(" ");
    Route r = new Route(color(int(colorValues[0]), int(colorValues[1]), int(colorValues[2])));
    routes.add(r);

    int numberOfStations = int(txt[currentIndex++]);
    for (int j=0; j<numberOfStations; j++) {
      String[] currentAndNeighbors = txt[currentIndex++].split("-");
      String current = currentAndNeighbors[0];
      String[] neighbors = currentAndNeighbors[1].split(",");
      Station currentStation = getStation(current);
      Station[] neighborStations = new Station[neighbors.length];
      for (int k=0; k<neighbors.length; k++) {
        neighborStations[k] = getStation(neighbors[k]);
      }
      r.addIntersection(currentStation, neighborStations);
    }
  }
}

void loadRoads() {
  String[] txt = loadStrings("Data/Saved Info/roads.txt");
  roads = new PVector[txt.length];
  roadNodes = new Node[txt.length];
  for (int i=0; i<txt.length; i++) {
    String[] coordinates = txt[i].split(" ");
    PVector r = new PVector(int(coordinates[0]), int(coordinates[1]));
    roads[i] = r;
    roadNodes[i] = new Node(r);
  }
}

void loadPlaces() {
  tagToIndex.put("COMPOUND", 0);
  tagToIndex.put("MALL", 1);
  tagToIndex.put("PARK", 2);
  tagToIndex.put("HOTEL", 3);
  tagToIndex.put("MUSEUM", 4);
  tagToIndex.put("STADIUM", 5);
  tagToIndex.put("AIRPORT", 6);
  tagToIndex.put("INSTITUTION", 7);
  tagToIndex.put("DISTRICT", 8);

  String[] txt = loadStrings("Data/Saved Info/places.txt");
  places = new Place[txt.length/3];
  for (int i=0; i<places.length; i++) {
    String name = txt[i*3];
    String tag = txt[i*3+1];
    String[] coordinates = txt[i*3+2].split(" ");
    places[i] = new Place(name, tag, int(coordinates[0]), int(coordinates[1]));
  }

  pinIcon = loadImage("Data/Images/pin icon.png");
  for (int i=0; i<placeIcons.length; i++) {
    placeIcons[i] = loadImage("Data/Images/place icon "+i+".png");
  }
}
