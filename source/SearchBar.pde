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

  void display() {
    x = width*xr+90;
    y = yr;
    w = width*0.4;
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
    text(searchTerm, x-w*0.48, y-h*0.10);

    // If the search bar is active, draw the dropdown list
    if (active) {
      drawDropdown();
    }
  }

  void drawDropdown() {
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
      rect(x-w/2+8, y+h+h*(index-0.42), w-70, h*0.8, 10);
    }

    // Draw the matches
    textAlign(LEFT, CENTER);
    textSize(20);
    for (int i = startIndex; i < endIndex; i++) {
      Place p = matches.get(i);
      p.display(x-w/2+30, y+h/2+(i-startIndex+0.5)*h);

      noStroke();
      fill(0);
      text(p.name, x-w/2+60, y+h/2+(i-startIndex+0.5)*h);
    }
  }

  void reset() {
    active = false;
    hasFocus = false;
    searchTerm = "";
    startIndex = 0;
    result = null;
  }

  void enterKey(char c) {
    if (hasFocus) {
      startIndex = 0;
      searchTerm += c;
      active = true;
    }
  }

  boolean interactWithKeyboard() {
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

  void disable() {
    hasFocus = false;
    active = false;
    startIndex = 0;
  }

  boolean interactWithMouse() {
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

  boolean interactWithMouseWheel(int dir) {
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
