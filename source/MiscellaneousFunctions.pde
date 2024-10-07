float distSq(float x1, float y1, float x2, float y2) {
  return (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);
}

float distSq(PVector p1, PVector p2) {
  return distSq(p1.x, p1.y, p2.x, p2.y);
}

color darker(color c) {
  return color(red(c)*0.8, green(c)*0.8, blue(c)*0.8);
}

Station getStation(String name) {
  for (Station s : stations) {
    if (s.name.equals(name)) return s;
  }
  return null;
}

boolean mouseInRect(float minX, float maxX, float minY, float maxY) {
  return mouseX>=minX && mouseX<=maxX && mouseY>=minY && mouseY<=maxY;
}

void constrainCamera() {
  float paddAmountX = (backgroundDisplay.width*2.66-width/targetZoomLevel)/2;
  float paddAmountY = (backgroundDisplay.height*2.66-height/targetZoomLevel)/2;

  targetCameraCenter.x = constrain(targetCameraCenter.x, referenceDisplay.width/2-paddAmountX, referenceDisplay.width/2+paddAmountX-30);
  targetCameraCenter.y = constrain(targetCameraCenter.y, referenceDisplay.height/2-paddAmountY, referenceDisplay.height/2+paddAmountY-35);
}

void resetPath() {
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

void assignPoint(PVector p, boolean isStart) {
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

Route getRoute(Station s1, Station s2) {
  for (Route r : routes) {
    if (r.contains(s1) && r.contains(s2)) {
      if(r.isLinked(s1,s2)) return r;
    }
  }
  return null;
}

void findPath() {
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
