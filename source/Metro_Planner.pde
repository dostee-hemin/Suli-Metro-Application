import java.util.*;
import java.io.*;

PVector[] roads;
ArrayList<Station> stations = new ArrayList<Station>();
ArrayList<Route> routes = new ArrayList<Route>();
Place[] places;
float minZoom = 0.5;
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

void setup() {
  size(1280,760, P2D);
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
  searchBars[1] = new SearchBar(0.5, 76, false);
}

void draw() {
  if (prevWidth != width || prevHeight != height) constrainCamera();
  prevWidth = width;
  prevHeight = height;
  background(#f2f2f2);

  pushMatrix();

  translate(width/2, height/2);
  scale(zoomLevel);
  translate(-cameraCenter.x, -cameraCenter.y);


  imageMode(CORNER);
  image(backgroundDisplay, -30-1053.125,-35-586.84216,backgroundDisplay.width*2.66,backgroundDisplay.height*2.66);
  image(displayMap, -30, -35, referenceDisplay.width, referenceDisplay.height);
  
  // println(mouseX+" , "+mouseY);
  // stroke(255,0,0);
  // strokeWeight(5);
  // point(mouseX,mouseY);
  zoomLevel = lerp(zoomLevel, targetZoomLevel, 0.2);
  cameraCenter = PVector.lerp(cameraCenter, targetCameraCenter, 0.2);


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

  panelX = lerp(panelX, isShowingDirections?targetPanelX:0, 0.1);

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
      float y = 100 + (i-0.5)*150;
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
      float kmDistance = totalDistance / 1000.0;
      formattedDistance = String.format("%.2f km", kmDistance);
    }

    textSize(20);
    textAlign(LEFT);
    text("Total time: " + formattedTime, width-panelX+50, 50+directions.size()*150);
    text("Total distance: " + formattedDistance, width-panelX+50, 50+(directions.size()+0.3)*150);
    popMatrix();

    noFill();
    stroke(0);
    strokeWeight(5);
    rectMode(CORNER);
    rect(width-panelX, 0, targetPanelX, height);
  }
}
