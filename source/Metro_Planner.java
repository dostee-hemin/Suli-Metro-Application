/* autogenerated by Processing revision 1293 on 2024-10-07 */
import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import java.util.*;
import java.io.*;
import java.util.HashMap;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class Metro_Planner extends PApplet {




PVector[] roads;
ArrayList<Station> stations = new ArrayList<Station>();
ArrayList<Route> routes = new ArrayList<Route>();
Place[] places;
float minZoom = 0.5f;
float maxZoom = 7;
float zoomLevel = 1;
float targetZoomLevel = 1;
PVector cameraCenter;
PVector targetCameraCenter;
float thresh = 2;
int meterPerPixel = 12;

PVector startPoint;
PVector endPoint;
ArrayList<PVector> firstWalkingPath = new ArrayList<PVector>();
ArrayList<PVector> walkOnlyPath = new ArrayList<PVector>();
ArrayList<PVector> lastWalkingPath = new ArrayList<PVector>();
ArrayList<Station> metroPath = new ArrayList<Station>();
ArrayList<Direction> directions = new ArrayList<Direction>();
ArrayList<Thread> allThreads = new ArrayList<Thread>();

Node[] roadNodes;
ArrayList<Node> stationNodes = new ArrayList<Node>();
SearchBar[] searchBars = new SearchBar[2];

boolean isPlacingA;
boolean isPlacingB;
boolean isShowingMetro;

float panelX;
float panelY;
float targetPanelX = 400;
boolean isShowingDirections;

int prevWidth, prevHeight;

public void setup() {
  /* size commented out by preprocessor */;
  //size(1890, 1060, P2D);

  loadImages();
  loadMetroRoute();
  loadRoads();
  loadPlaces();

  surface.setTitle("Metro Routing Application");
  surface.setResizable(true);

  textFont(createFont("Nexa-Heavy.ttf", 96));

  cameraCenter = new PVector(referenceDisplay.width/2, referenceDisplay.height/2);
  targetCameraCenter = new PVector(referenceDisplay.width/2, referenceDisplay.height/2);

  searchBars[0] = new SearchBar(0, 76, true);
  searchBars[1] = new SearchBar(0.5f, 76, false);
}

public void draw() {
  if (prevWidth != width || prevHeight != height) constrainCamera();
  prevWidth = width;
  prevHeight = height;
  background(0xFFF2F2F2);

  pushMatrix();

  translate(width/2, height/2);
  scale(zoomLevel);
  translate(-cameraCenter.x, -cameraCenter.y);


  imageMode(CORNER);
  image(backgroundDisplay, -30-1053.125f,-35-586.84216f,backgroundDisplay.width*2.66f,backgroundDisplay.height*2.66f);
  image(displayMap, -30, -35, referenceDisplay.width, referenceDisplay.height);
  
  // println(mouseX+" , "+mouseY);
  // stroke(255,0,0);
  // strokeWeight(5);
  // point(mouseX,mouseY);
  zoomLevel = lerp(zoomLevel, targetZoomLevel, 0.2f);
  cameraCenter = PVector.lerp(cameraCenter, targetCameraCenter, 0.2f);


  imageMode(CENTER);
  textAlign(CENTER, CENTER);
  if (isPlacingA || isPlacingB) {
    int minDist = 20;
    PVector closest = null;
    PVector mouse = new PVector(mouseX, mouseY);
    PVector centerToMouse = PVector.sub(mouse, new PVector(width/2, height/2));
    centerToMouse.div(zoomLevel);
    PVector mouseScaled = PVector.add(centerToMouse, cameraCenter);
    float recordDist = Float.MAX_VALUE;
    for (PVector p : roads) {
      if (p.x > mouseScaled.x-minDist && p.x < mouseScaled.x+minDist && p.y > mouseScaled.y-minDist && p.y < mouseScaled.y+minDist) {
        float distanceToMouse = distSq(p.x, p.y, mouseScaled.x, mouseScaled.y);
        if (distanceToMouse < recordDist) {
          recordDist = distanceToMouse;
          closest = p;
        }
      }
    }
    textSize(20);
    noStroke();
    if (closest != null) {
      pushMatrix();
      translate(closest.x, closest.y-16);
      fill(255, 0, 0);
      ellipse(0, -5, 16, 16);
      image(pinIcon, 0, 0, 32, 32);
      fill(255);
      text(isPlacingA ? "A" : "B", 0, -7);
      popMatrix();
    }
  }
  noStroke();
  if (startPoint != null) {
    pushMatrix();
    translate(startPoint.x, startPoint.y-16);
    fill(255, 0, 0);
    ellipse(0, -4, 22, 22);
    image(pinIcon, 0, 0, 32, 32);
    fill(255);
    textSize(20);
    text("A", 0, -7);
    popMatrix();
  }

  if (endPoint != null) {
    pushMatrix();
    translate(endPoint.x, endPoint.y-16);
    fill(255, 0, 0);
    ellipse(0, -4, 22, 22);
    image(pinIcon, 0, 0, 32, 32);
    fill(255);
    textSize(20);
    text("B", 0, -7);
    popMatrix();
  }

  if (isShowingMetro) {
    for (Route r : routes) r.display();
  }

  if (!walkOnlyPath.isEmpty()) {
    if (directions.isEmpty()) {
      directions.add(new WalkingDirection("Start", "End", walkOnlyPath));
      isShowingDirections = true;
    }
  }

  if (!firstWalkingPath.isEmpty() && !lastWalkingPath.isEmpty() && !metroPath.isEmpty()) {
    if (directions.isEmpty()) {
      directions.add(new WalkingDirection("Start", metroPath.get(0).name, firstWalkingPath));
      Station originStation = metroPath.get(0);
      Route previousRoute = getRoute(originStation, metroPath.get(1));
      ArrayList<Station> routeStations = new ArrayList<Station>();
      routeStations.add(originStation);
      int numStops = 0;
      float totalDistance = 0;
      for (int i=1; i<metroPath.size(); i++) {
        Station currentStation = metroPath.get(i);
        Station previousStation = metroPath.get(i-1);
        Route currentRoute = getRoute(originStation, currentStation);
        if (currentRoute == null) {
          currentRoute = getRoute(previousStation, currentStation);
        }

        if (previousRoute == currentRoute) {
          numStops++;
          totalDistance += PVector.dist(previousStation.position, currentStation.position);
          routeStations.add(currentStation);
          continue;
        }

        directions.add(new MetroDirection(routeStations, numStops, totalDistance, previousRoute.lineColor));
        originStation = previousStation;
        routeStations = new ArrayList<Station>();
        routeStations.add(originStation);
        previousRoute = currentRoute;
        numStops = 0;
        totalDistance = 0;
        i--;
      }
      Route finalRoute = getRoute(metroPath.get(metroPath.size()-2), metroPath.get(metroPath.size()-1));
      directions.add(new MetroDirection(routeStations, numStops, totalDistance, finalRoute.lineColor));
      directions.add(new WalkingDirection(metroPath.get(metroPath.size()-1).name, "End", lastWalkingPath));
      isShowingDirections = true;
    }
  }

  for (Direction d : directions) d.displayMap();
  popMatrix();


  for (SearchBar s : searchBars) s.display();
  fill(255);
  stroke(50);
  strokeWeight(3);
  rectMode(CENTER);
  rect(50, 100, 60, 60, 10);
  rect(width/2+50, 100, 60, 60, 10);
  rect(50, height-100, 60, 60, 10);
  rect(50, height-170, 60, 60, 10);
  fill(255, 0, 0);
  noStroke();
  ellipse(50, 90, 30, 30);
  ellipse(width/2+50, 90, 30, 30);
  image(pinIcon, 50, 100, 50, 50);
  image(pinIcon, width/2+50, 100, 50, 50);
  image(directionIcons[1], 50, height-100, 50, 50);
  image(resetIcon, 50, height-170, 50, 50);
  fill(255);
  textSize(33);
  textAlign(CENTER, CENTER);
  text("A", 50, 88);
  text("B", width/2+50, 88);
  fill(0, isPlacingA?0:50);
  rect(50, 100, 60, 60, 10);
  fill(0, isPlacingB?0:50);
  rect(width/2+50, 100, 60, 60, 10);
  fill(0, isShowingMetro?0:50);
  rect(50, height-100, 60, 60, 10);

  panelX = lerp(panelX, isShowingDirections?targetPanelX:0, 0.1f);

  if (!directions.isEmpty()) {
    fill(230);
    stroke(0);
    strokeWeight(5);
    rectMode(CORNER);
    rect(width-panelX-50, height/2-40, 60, 80, 10);

    fill(240);
    noStroke();
    rect(width-panelX, 0, targetPanelX, height);
    fill(248);
    rect(width-panelX+targetPanelX-40, 0, 40, height);
    if (directions.size()*150+120 > height) {
      float y = map(panelY, height-(directions.size()*150+120), 0, height-60, 60);
      fill(200);
      rectMode(CENTER);
      rect(width-panelX+targetPanelX-20, y, 30, 100, 10);
    }

    stroke(120);
    strokeWeight(8);
    if (isShowingDirections) {
      line(width-panelX-35, height/2, width-panelX-20, height/2-15);
      line(width-panelX-35, height/2, width-panelX-20, height/2+15);
    } else {
      line(width-panelX-20, height/2, width-panelX-35, height/2-15);
      line(width-panelX-20, height/2, width-panelX-35, height/2+15);
    }

    pushMatrix();
    translate(0, panelY);
    int totalTime = 0;
    int totalDistance = 0;
    for (int i=0; i<directions.size(); i++) {
      Direction d = directions.get(i);
      float x = width-panelX;
      float y = 100 + (i-0.5f)*150;
      d.displayPanel(x, y);
      totalTime += d.time;
      totalDistance += d.distance;
    }

    int hours = totalTime / 3600;
    int minutes = (totalTime % 3600) / 60;
    String formattedTime = "";
    if (hours > 0) {
      formattedTime += hours + " hrs ";
    }
    if (minutes > 1) {
      formattedTime += minutes + " mins";
    } else formattedTime = "~1min";

    String formattedDistance = "";
    if (totalDistance < 1000) {
      formattedDistance = totalDistance + " m";
    } else {
      float kmDistance = totalDistance / 1000.0f;
      formattedDistance = String.format("%.2f km", kmDistance);
    }

    textSize(20);
    textAlign(LEFT);
    text("Total time: " + formattedTime, width-panelX+50, 50+directions.size()*150);
    text("Total distance: " + formattedDistance, width-panelX+50, 50+(directions.size()+0.3f)*150);
    popMatrix();

    noFill();
    stroke(0);
    strokeWeight(5);
    rectMode(CORNER);
    rect(width-panelX, 0, targetPanelX, height);
  }
}
class Direction {
  String path;
  int distance;
  float time;

  Direction(String origin, String destination) {
    path = origin + "  →  " + destination;
  }

  public String getDistance() {
    return "woopsie";
  }

  public void displayMap() {
  }

  public void displayPanel(float x, float y) {
    fill(255, 100);
    stroke(200);
    strokeWeight(3);
    rectMode(CORNER);
    rect(x+10, y, targetPanelX-60, 140, 10);

    fill(50);
    textSize(40);
    textAlign(CENTER, CENTER);
    text(path, (2*x+50+targetPanelX)/2, y+40);

    int hours = PApplet.parseInt(time) / 3600;
    int minutes = (PApplet.parseInt(time) % 3600) / 60;
    String formattedTime = "";
    if (hours > 0) {
      formattedTime += hours + " hrs ";
    }
    if (minutes > 1) {
      formattedTime += minutes + " mins";
    } else formattedTime = "~1min";
    textSize(25);
    textAlign(LEFT);
    text(formattedTime, x+30, y+125);
    textAlign(RIGHT);
    text(getDistance(), x+targetPanelX-90, y+125);
  }
}

class WalkingDirection extends Direction {
  ArrayList<PVector> path;

  WalkingDirection(String origin, String destination, ArrayList<PVector> path) {
    super(origin, destination);
    distance = path.size() * meterPerPixel;
    time = distance / 1.4f;
    this.path = path;
  }

  @Override public 
    String getDistance() {
    String formattedDistance = "";
    if (distance < 1000) {
      formattedDistance = distance + " m";
    } else {
      float kmDistance = distance / 1000.0f;
      formattedDistance = String.format("%.2f km", kmDistance);
    }
    return formattedDistance;
  }

  @Override public 
    void displayMap() {
    for (int i=0; i<path.size(); i+=map(zoomLevel, minZoom, maxZoom, 20, 5)) {
      PVector p = path.get(i);
      fill(0xFF00B0FF);
      stroke(0xFF1967D2);
      strokeWeight(map(zoomLevel, minZoom, maxZoom, 2, 0.5f));
      ellipse(p.x, p.y, map(zoomLevel, minZoom, maxZoom, 5, 3), map(zoomLevel, minZoom, maxZoom, 5, 3));
    }
  }

  @Override public 
    void displayPanel(float x, float y) {
    super.displayPanel(x, y);

    imageMode(CENTER);
    image(directionIcons[0], x+70, y+50, 80, 80);
  }
}

class MetroDirection extends Direction {
  int numStops;
  int tagColor;
  ArrayList<Station> routeStations;

  MetroDirection(ArrayList<Station> routeStations, int numStops, float distance, int tagColor) {
    super(routeStations.get(0).name, routeStations.get(routeStations.size()-1).name);
    this.numStops = numStops;
    this.tagColor = tagColor;
    time = (distance*meterPerPixel) / 16.7f + (numStops * 10);
    this.distance = PApplet.parseInt(distance*meterPerPixel);
    this.routeStations = routeStations;
  }

  @Override public 
    String getDistance() {
    if (numStops == 1) return "1 stop";
    return numStops + " stops";
  }

  @Override public 
    void displayMap() {
    for (int i=0; i<routeStations.size()-1; i++) {
      Station p1 = routeStations.get(i);
      Station p2 = routeStations.get(i+1);

      stroke(darker(tagColor));
      strokeWeight(5);
      line(p1.position.x, p1.position.y, p2.position.x, p2.position.y);
      stroke(tagColor);
      strokeWeight(2);
      line(p1.position.x, p1.position.y, p2.position.x, p2.position.y);

      strokeWeight(2);
      stroke(0);
      fill(255);
      ellipse(p1.position.x, p1.position.y, 5, 5);
      ellipse(p2.position.x, p2.position.y, 5, 5);
    }
  }

  @Override public 
    void displayPanel(float x, float y) {
    super.displayPanel(x, y);

    imageMode(CENTER);
    image(directionIcons[1], x+70, y+50, 80, 80);
    fill(tagColor);
    noStroke();
    rectMode(CENTER);
    rect(x+70, y+90, 50, 10);
  }
}
class MetroPathFinder extends Thread {
  ArrayList<Station> path = new ArrayList<Station>();
  ArrayList<Node> openSet = new ArrayList<Node>();
  ArrayList<Node> closedSet = new ArrayList<Node>();
  Node startNode;
  Node endNode;

  MetroPathFinder(ArrayList<Station> path, Station A, Station B) {
    this.path = path;

    for (Node n : stationNodes) {
      if (n.station == A) startNode = n;
      if (n.station == B) endNode = n;
    }

    openSet.add(startNode);
  }

  @Override
    public void run() {
    try {
      while (openSet.size() > 0) {
        startNode.parent = null;
        Node currentNode = openSet.get(0);
        for (int i = 1; i < openSet.size(); i++) {
          if (openSet.get(i).f < currentNode.f) {
            currentNode = openSet.get(i);
          }
        }

        if (currentNode == endNode) {
          Node current = currentNode;
          while (current != null) {
            path.add(current.station);
            current = current.parent;
          }
          Collections.reverse(path);
          return;
        }

        openSet.remove(currentNode);
        closedSet.add(currentNode);

        for (int i=0; i<stations.size(); i++) {
          Node neighbor = stationNodes.get(i);

          boolean isNeighbor = false;
          for (Station s : currentNode.station.connectingStations) {
            if (neighbor.station == s) isNeighbor = true;
          }
          if (!isNeighbor) continue;

          if (closedSet.contains(neighbor)) continue;

          float tentativeGScore = currentNode.g + distSq(currentNode.position, neighbor.position);
          boolean newNeighbor = !openSet.contains(neighbor);
          if (newNeighbor || tentativeGScore < neighbor.g) {
            neighbor.parent = currentNode;
            neighbor.g = tentativeGScore;
            neighbor.calculateHeuristic(endNode);
            neighbor.calculateScore();
            if (newNeighbor) {
              openSet.add(neighbor);
            }
          }
        }
      }
    }
    catch (Exception e) {
    }
  }
}
public float distSq(float x1, float y1, float x2, float y2) {
  return (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);
}

public float distSq(PVector p1, PVector p2) {
  return distSq(p1.x, p1.y, p2.x, p2.y);
}

public int darker(int c) {
  return color(red(c)*0.8f, green(c)*0.8f, blue(c)*0.8f);
}

public Station getStation(String name) {
  for (Station s : stations) {
    if (s.name.equals(name)) return s;
  }
  return null;
}

public boolean mouseInRect(float minX, float maxX, float minY, float maxY) {
  return mouseX>=minX && mouseX<=maxX && mouseY>=minY && mouseY<=maxY;
}

public void constrainCamera() {
  float paddAmountX = (backgroundDisplay.width*2.66f-width/targetZoomLevel)/2;
  float paddAmountY = (backgroundDisplay.height*2.66f-height/targetZoomLevel)/2;

  targetCameraCenter.x = constrain(targetCameraCenter.x, referenceDisplay.width/2-paddAmountX, referenceDisplay.width/2+paddAmountX-30);
  targetCameraCenter.y = constrain(targetCameraCenter.y, referenceDisplay.height/2-paddAmountY, referenceDisplay.height/2+paddAmountY-35);
}

public void resetPath() {
  for (Thread t : allThreads) t.interrupt();
  allThreads = new ArrayList<Thread>();
  firstWalkingPath = new ArrayList<PVector>();
  lastWalkingPath = new ArrayList<PVector>();
  metroPath = new ArrayList<Station>();
  directions = new ArrayList<Direction>();
  walkOnlyPath = new ArrayList<PVector>();
  panelX = 0;
  panelY = 0;
  isShowingDirections = false;
}

public void assignPoint(PVector p, boolean isStart) {
  PVector closestRoad = null;
  float recordDist = Float.MAX_VALUE;
  for (PVector r : roads) {
    float distance = distSq(r, p);
    if (distance < recordDist) {
      recordDist = distance;
      closestRoad = r;
    }
  }
  if (isStart) startPoint = closestRoad;
  else endPoint = closestRoad;

  if (startPoint != null && endPoint != null) {
    findPath();
  }
}

public Route getRoute(Station s1, Station s2) {
  for (Route r : routes) {
    if (r.contains(s1) && r.contains(s2)) {
      if(r.isLinked(s1,s2)) return r;
    }
  }
  return null;
}

public void findPath() {
  resetPath();

  Station metroStart = null;
  Station metroEnd = null;
  float recordStartDistance = Float.MAX_VALUE;
  float recordEndDistance = Float.MAX_VALUE;
  for (Station s : stations) {
    float startToStation = distSq(startPoint, s.position);
    float endToStation = distSq(endPoint, s.position);

    if (startToStation < recordStartDistance) {
      recordStartDistance = startToStation;
      metroStart = s;
    }

    if (endToStation < recordEndDistance) {
      recordEndDistance = endToStation;
      metroEnd = s;
    }
  }

  // If there is no need for a metro to go from point A to B
  if (metroStart == metroEnd) {
    Thread walkOnlyThread = new WalkingPathFinder(walkOnlyPath, startPoint, endPoint);
    allThreads.add(walkOnlyThread);
    walkOnlyThread.start();
    return;
  }

  PVector pathStart = null;
  PVector pathEnd = null;
  recordStartDistance = Float.MAX_VALUE;
  recordEndDistance = Float.MAX_VALUE;
  for (PVector p : roads) {
    float startToStation = distSq(metroStart.position, p);
    float endToStation = distSq(metroEnd.position, p);

    if (startToStation < recordStartDistance) {
      recordStartDistance = startToStation;
      pathStart = p;
    }

    if (endToStation < recordEndDistance) {
      recordEndDistance = endToStation;
      pathEnd = p;
    }
  }

  Thread firstWalkThread = new WalkingPathFinder(firstWalkingPath, startPoint, pathStart);
  Thread lastWalkThread = new WalkingPathFinder(lastWalkingPath, pathEnd, endPoint);
  Thread metroThread = new MetroPathFinder(metroPath, metroStart, metroEnd);

  allThreads.add(firstWalkThread);
  allThreads.add(lastWalkThread);
  allThreads.add(metroThread);
  firstWalkThread.start();
  lastWalkThread.start();
  metroThread.start();
}
class Node {
  PVector position;
  float f, g, h;
  Node parent;
  Station station;
  
  Node(Station s) {
    this(s.position);
    station = s;
  }

  Node(PVector pos) {
    position = pos;
    f = 0;
    g = 0;
    h = 0;
    parent = null;
  }
  
  public void calculateHeuristic(Node goal) {
    h = distSq(position, goal.position);
  }
  
  public void calculateScore() {
    f = g + h;
  }
}


HashMap<String, Integer> tagToIndex = new HashMap<String, Integer>();

class Place {
  String name;
  String tag;
  int tagColor;
  PVector location;
  Place(String name, String tag, float x, float y) {
    this.name = name;
    this.tag = tag;
    location = new PVector(x, y);
  }

  public void display(float x, float y) {
    imageMode(CENTER);
    image(placeIcons[tagToIndex.get(tag)], x,y, 44, 44);
  }
}
PImage displayMap;
PImage referenceDisplay;
PImage backgroundDisplay;
PImage pinIcon;
PImage resetIcon;
PImage[] placeIcons = new PImage[9];
PImage[] directionIcons = new PImage[2];

public void loadImages() {
  displayMap = loadImage("Data/Images/display.png");
  referenceDisplay = loadImage("Data/Images/old display.png");
  backgroundDisplay = loadImage("Data/Images/background display.png");
  directionIcons[0] = loadImage("Data/Images/direction 0.png");
  directionIcons[1] = loadImage("Data/Images/direction 1.png");
  resetIcon = loadImage("Data/Images/reset.png");
}

public void loadMetroRoute() {
  String[] txt = loadStrings("Data/Saved Info/route details.txt");
  int numStations = PApplet.parseInt(txt[0]);
  for (int i=0; i<numStations; i++) {
    String name = txt[i*2+1];
    String[] coordinates = txt[i*2+2].split(" ");
    stations.add(new Station(name, PApplet.parseInt(coordinates[0]), PApplet.parseInt(coordinates[1])));
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
  int numRoutes = PApplet.parseInt(txt[currentIndex++]);
  for (int i=0; i<numRoutes; i++) {
    String[] colorValues = txt[currentIndex++].split(" ");
    Route r = new Route(color(PApplet.parseInt(colorValues[0]), PApplet.parseInt(colorValues[1]), PApplet.parseInt(colorValues[2])));
    routes.add(r);

    int numberOfStations = PApplet.parseInt(txt[currentIndex++]);
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

public void loadRoads() {
  String[] txt = loadStrings("Data/Saved Info/roads.txt");
  roads = new PVector[txt.length];
  roadNodes = new Node[txt.length];
  for (int i=0; i<txt.length; i++) {
    String[] coordinates = txt[i].split(" ");
    PVector r = new PVector(PApplet.parseInt(coordinates[0]), PApplet.parseInt(coordinates[1]));
    roads[i] = r;
    roadNodes[i] = new Node(r);
  }
}

public void loadPlaces() {
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
    places[i] = new Place(name, tag, PApplet.parseInt(coordinates[0]), PApplet.parseInt(coordinates[1]));
  }

  pinIcon = loadImage("Data/Images/pin icon.png");
  for (int i=0; i<placeIcons.length; i++) {
    placeIcons[i] = loadImage("Data/Images/place icon "+i+".png");
  }
}
class Route {
  ArrayList<Intersection> intersections = new ArrayList<Intersection>();
  int lineColor;

  Route(int c) {
    this.lineColor = c;
  }

  public void addIntersection(Station current, Station[] neighbors) {
    intersections.add(new Intersection(current, neighbors));
  }

  public void display() {
    for (Intersection i : intersections) {
      stroke(lineColor);
      strokeWeight(directions.isEmpty()?4:2);
      for (Station n : i.neighbors) {
        line(i.current.position.x, i.current.position.y, n.position.x, n.position.y);
      }
    }

    for (Intersection i : intersections) {
      i.current.display();
    }
  }
  
  public boolean contains(Station s) {
    for(Intersection i : intersections) {
      if(i.current == s) return true;
    }
    return false;
  }
  
  
  public boolean isLinked(Station s1, Station s2) {
    for(Intersection i : intersections) {
      if(i.current == s1) {
        for(Station neighbor : i.neighbors) {
          if(neighbor == s2) return true;
        }
      }
    }
    return false;
  }
}

class Intersection {
  Station current;
  Station[] neighbors;

  Intersection(Station current, Station[] neighbors) {
    this.current = current;
    this.neighbors = neighbors;
  }
}
class SearchBar {
  float x, y, w, h;          // position and size of the search bar
  float xr, yr;              // raito of position to canvas dimensions
  ArrayList<Place> matches = new ArrayList<Place>(); // list of matching strings
  String searchTerm = "";    // current search term
  PVector result;

  boolean active = false;    // whether the search bar is active
  boolean hasFocus = false;  // whether the search bar has keyboard focus
  int startIndex = 0;
  boolean isStart;

  SearchBar(float xr, float yr, boolean isStart) {
    this.xr = xr;
    this.yr = yr;
    h = 50;
    this.isStart = isStart;
  }

  public void display() {
    x = width*xr+90;
    y = yr;
    w = width*0.4f;
    // Draw the search bar
    stroke(0);
    fill(active ? 255 : 230);
    strokeWeight(5);
    rectMode(CORNER);
    rect(x, y, w, h, 10);
    
    x+=w/2;
    y+=h/2;

    // Draw the current search term
    textAlign(LEFT, CENTER);
    textSize(23);
    fill(0);
    text(searchTerm, x-w*0.48f, y-h*0.10f);

    // If the search bar is active, draw the dropdown list
    if (active) {
      drawDropdown();
    }
  }

  public void drawDropdown() {
    // Find the matches for the current search term
    matches.clear();
    for (Place p : places) {
      if (p.name.toLowerCase().contains(searchTerm.toLowerCase())) {
        matches.add(p);
      }
    }

    int endIndex = min(matches.size(), startIndex + 5);

    // Draw the dropdown list
    stroke(0);
    strokeWeight(3);
    fill(240);
    rectMode(CORNER);
    rect(x-w/2, y+h/2, w, min(matches.size(), 5)*h);

    if (matches.size() > 5) {
      //Draw the scroll bar
      rectMode(CENTER);
      noStroke();
      fill(230);
      rect(x+w/2-26, y+h+(min(matches.size(), 5)/2)*h, 48, min(matches.size(), 5)*h-4);
      stroke(150);
      strokeWeight(3);
      fill(200);
      rect(x+w/2-25, y+h/2+map(startIndex, 0, matches.size()-5, 40, 5*h-40), 20, 60, 8);
    }

    int index = (int) min(((mouseY-y-h/2) / h), matches.size()-1);
    if (index >= 0 && index < 5 && mouseX<x+w/2-49 && !matches.isEmpty()) {
      fill(250);
      stroke(245);
      strokeWeight(1);
      rectMode(CORNER);
      rect(x-w/2+8, y+h+h*(index-0.42f), w-70, h*0.8f, 10);
    }

    // Draw the matches
    textAlign(LEFT, CENTER);
    textSize(20);
    for (int i = startIndex; i < endIndex; i++) {
      Place p = matches.get(i);
      p.display(x-w/2+30, y+h/2+(i-startIndex+0.5f)*h);

      noStroke();
      fill(0);
      text(p.name, x-w/2+60, y+h/2+(i-startIndex+0.5f)*h);
    }
  }

  public void reset() {
    active = false;
    hasFocus = false;
    searchTerm = "";
    startIndex = 0;
    result = null;
  }

  public void enterKey(char c) {
    if (hasFocus) {
      startIndex = 0;
      searchTerm += c;
      active = true;
    }
  }

  public boolean interactWithKeyboard() {
    // Handle special keys
    if (hasFocus) {
      if (keyCode == BACKSPACE) {
        searchTerm = searchTerm.substring(0, max(searchTerm.length()-1, 0));
        active = true;
        return true;
      } else if (keyCode == ENTER) {
        hasFocus = false;
        active = false;
        return true;
      }
    }

    return false;
  }

  public void disable() {
    hasFocus = false;
    active = false;
    startIndex = 0;
  }

  public boolean interactWithMouse() {
    // Check if the mouse is inside the search bar or dropdown list
    if (mouseInRect(x-w/2, x+w/2, y-h/2, y+h/2)) {
      for (SearchBar s : searchBars) s.disable();
      if (searchTerm.equals("CUSTOM")) searchTerm = "";
      hasFocus = true;
      active = true;
      return true;
    } else if (active && mouseInRect(x-w/2, x+w/2-49, y-h/2, y+h/2+min(5, matches.size())*h)) {
      // Clicked on a match
      int index = (int) ((mouseY-y-h/2) / h);
      index += startIndex;
      Place p = matches.get(index);
      result = p.location;
      searchTerm = p.name;
      hasFocus = false;
      active = false;
      startIndex = 0;
      assignPoint(result, isStart);
      return true;
    } else if (active && mouseInRect(x-w/2, x+w/2, y-h/2, y+h/2+min(5, matches.size())*h) && !matches.isEmpty()) {
      return true;
    } else if (active || hasFocus) {
      disable();
      return true;
    }
    return false;
  }

  public boolean interactWithMouseWheel(int dir) {
    if (matches.size() < 5) return false;

    if (active && mouseInRect(x-w/2, x+w/2, y-h/2, y+h/2+min(5, matches.size())*h)) {
      // Clicked on a match
      startIndex += dir;
      startIndex = constrain(startIndex, 0, matches.size()-5);
      return true;
    }
    return false;
  }
}
class Station {
  String name;
  ArrayList<Station> connectingStations = new ArrayList<Station>();
  PVector position;

  Station(String name, float xPosition, float yPosition) {
    this.name = name;
    position = new PVector(xPosition, yPosition);
  }

  public void addConnection(Station s) {
    connectingStations.add(s);
  }

  public void display() {
    strokeWeight(15);
    stroke(50);
    point(position.x, position.y);
    fill(0);
    textSize(20);
    textAlign(CENTER);
    text(name, position.x, position.y - 20);
  }
}
public void keyTyped() {
  for (SearchBar s : searchBars) s.enterKey(key);
}
public void keyPressed() {
  for (SearchBar s : searchBars)
    if (s.interactWithKeyboard()) return;
}

public void mousePressed() {
  if (mouseButton == CENTER) return;


  for (SearchBar s : searchBars) {
    if (s.interactWithMouse()) {
      isPlacingA = false;
      isPlacingB = false;
      isShowingDirections = false;
      return;
    }
  }

  if (mouseInRect(20, 80, 70, 130)) {
    if (!isPlacingA) {
      isPlacingA = true;
      isPlacingB = false;
      startPoint = null;
      searchBars[0].reset();
      resetPath();
    } else {
      isPlacingA = false;
    }
    return;
  } else if (mouseInRect(width/2+20, width/2+80, 70, 130)) {
    if (!isPlacingB) {
      isPlacingB = true;
      isPlacingA = false;
      endPoint = null;
      searchBars[1].reset();
      resetPath();
    } else {
      isPlacingB = false;
    }
    return;
  }

  if (mouseInRect(20, 80, height-130, height-70)) {
    isShowingMetro = !isShowingMetro;
    return;
  } else if(mouseInRect(20, 80, height-200, height-140)) {
    for(SearchBar s : searchBars) s.reset();
    startPoint = null;
    endPoint = null;
    resetPath();
  }

  if (!directions.isEmpty()) {
    if (isShowingDirections && mouseInRect(width-panelX-50, width-panelX, height/2-40, height/2+40)) {
      isShowingDirections = false;
    } else if (!isShowingDirections && mouseInRect(width-50, width, height/2-40, height/2+40)) {
      isShowingDirections = true;
    }
    return;
  }

  if (isPlacingA || isPlacingB) {
    int minDist = 20;
    PVector closest = null;
    PVector mouse = new PVector(mouseX, mouseY);
    PVector centerToMouse = PVector.sub(mouse, new PVector(width/2, height/2));
    centerToMouse.div(zoomLevel);
    PVector mouseScaled = PVector.add(centerToMouse, cameraCenter);
    float recordDist = Float.MAX_VALUE;
    for (PVector p : roads) {
      if (p.x > mouseScaled.x-minDist && p.x < mouseScaled.x+minDist && p.y > mouseScaled.y-minDist && p.y < mouseScaled.y+minDist) {
        float distanceToMouse = distSq(p.x, p.y, mouseScaled.x, mouseScaled.y);
        if (distanceToMouse < recordDist) {
          recordDist = distanceToMouse;
          closest = p;
        }
      }
    }
    if (closest != null) {
      if (isPlacingA) {
        startPoint = closest;
        isPlacingA = false;
        searchBars[0].searchTerm = "CUSTOM";
      }
      if (isPlacingB) {
        endPoint = closest;
        isPlacingB = false;
        searchBars[1].searchTerm = "CUSTOM";
      }
    }

    if (startPoint != null && endPoint != null) {
      findPath();
    }
  }
}

public void mouseDragged() {
  if (mouseButton == CENTER) {
    PVector prev = new PVector(pmouseX, pmouseY);
    PVector curr = new PVector(mouseX, mouseY);
    PVector dir = PVector.sub(curr, prev);
    targetCameraCenter.add(dir.div(-zoomLevel));

    constrainCamera();
  }
}

public void mouseWheel(MouseEvent event) {
  float e = event.getCount();

  if (mouseInRect(width-panelX, width, 0, height) && isShowingDirections) {
    if (directions.size()*150+120 > height) {
      panelY += (e < 0 ? 35 : -35);
      panelY = constrain(panelY, height-(directions.size()*150+120), 0);
    }
    return;
  }


  for (SearchBar s : searchBars)
    if (s.interactWithMouseWheel((e < 0 ? -1 : 1))) return;

  targetZoomLevel *= (e < 0 ? 1.3f : 0.8f);
  targetZoomLevel = constrain(targetZoomLevel, minZoom, maxZoom);

  constrainCamera();
}
class WalkingPathFinder extends Thread {
  ArrayList<PVector> path = new ArrayList<PVector>();
  ArrayList<Node> openSet = new ArrayList<Node>();
  ArrayList<Node> closedSet = new ArrayList<Node>();
  Node startNode;
  Node endNode;

  WalkingPathFinder(ArrayList<PVector> path, PVector A, PVector B) {
    path.clear();
    this.path = path;

    for (Node n : roadNodes) {
      if (n.position == A) startNode = n;
      if (n.position == B) endNode = n;
    }

    openSet.add(startNode);
  }

  @Override
    public void run() {
    try {
      while (openSet.size() > 0) {
        Node currentNode = openSet.get(0);
        for (int i = 1; i < openSet.size(); i++) {
          if (openSet.get(i).f < currentNode.f) {
            currentNode = openSet.get(i);
          }
        }

        if (currentNode == endNode) {
          Node current = currentNode;
          while (current != null) {
            path.add(current.position);
            current = current.parent;
          }
          Collections.reverse(path);
          return;
        }

        openSet.remove(currentNode);
        closedSet.add(currentNode);

        int padding = 10;
        for (int i=0; i<roads.length; i++) {
          Node neighbor = roadNodes[i];
          if (neighbor.position.x < min(startNode.position.x, endNode.position.x) - padding ||
            neighbor.position.x > max(startNode.position.x, endNode.position.x) + padding ||
            neighbor.position.y < min(startNode.position.y, endNode.position.y) - padding ||
            neighbor.position.y > max(startNode.position.y, endNode.position.y) + padding)
            continue;

          if (distSq(currentNode.position, neighbor.position) > 1) continue;

          if (closedSet.contains(neighbor)) continue;

          float tentativeGScore = currentNode.g + distSq(currentNode.position, neighbor.position);
          boolean newNeighbor = !openSet.contains(neighbor);
          if (newNeighbor || tentativeGScore < neighbor.g) {
            neighbor.parent = currentNode;
            neighbor.g = tentativeGScore;
            neighbor.calculateHeuristic(endNode);
            neighbor.calculateScore();
            if (newNeighbor) {
              openSet.add(neighbor);
            }
          }
        }
      }
    }
    catch (Exception e) {
    }
  }
}


  public void settings() { size(1280, 760, P2D); }

  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Metro_Planner" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
