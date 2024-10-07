void keyTyped() {
  for (SearchBar s : searchBars) s.enterKey(key);
}
void keyPressed() {
  for (SearchBar s : searchBars)
    if (s.interactWithKeyboard()) return;
}

void mousePressed() {
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

void mouseDragged() {
  if (mouseButton == CENTER) {
    PVector prev = new PVector(pmouseX, pmouseY);
    PVector curr = new PVector(mouseX, mouseY);
    PVector dir = PVector.sub(curr, prev);
    targetCameraCenter.add(dir.div(-zoomLevel));

    constrainCamera();
  }
}

void mouseWheel(MouseEvent event) {
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

  targetZoomLevel *= (e < 0 ? 1.3 : 0.8);
  targetZoomLevel = constrain(targetZoomLevel, minZoom, maxZoom);

  constrainCamera();
}
