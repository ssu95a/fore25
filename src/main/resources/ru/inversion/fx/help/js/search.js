let rangeSelector;

function searchText(text, caseSensitive) {
    let count = 0;
    const options = {
        "caseSensitive": caseSensitive,
        "done": function(counter) {
            count = counter;
        }
    };
    $("body").mark(text, options);
    rangeSelector = new RangeSelector(document.getElementsByTagName('mark'));
    return count;
}

function searchTextRegEx(text, flags) {
    let count = 0;
    const options = {
        "done": function(counter) {
            count = counter;
        }
    };
    markRegEx(text, flags, options);
    return count;
}

function searchWord(text, flags) {
    let count = 0;
    const options = {
        "ignoreGroups": 1,
        "done": function(counter) {
            count = counter;
        }
    };
    markRegEx(text, flags, options);
    return count;
}

function markRegEx(text, flags, options) {
    const regexp = new RegExp(text, flags);
    $("body").markRegExp(regexp, options);
    rangeSelector = new RangeSelector(document.getElementsByTagName('mark'));
}

function clearMark() {
    $("body").unmark();
    rangeSelector = null;
}

function selectNext() {
    if (rangeSelector != null) {
        _highlightAndScroll(rangeSelector.nextRange());
    }
}

function selectPrev() {
    if (rangeSelector != null) {
        _highlightAndScroll(rangeSelector.prevRange())
    }
}

function _highlightAndScroll(element) {
    const elementRect = element.getBoundingClientRect();
    const absoluteElementTop = elementRect.top + window.pageYOffset;
    const middle = absoluteElementTop - (window.innerHeight / 2);
    window.scrollTo(0, middle);
    let rng, sel;
    if (document.createRange) {
        rng = document.createRange();
        rng.selectNode(element);
        sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(rng);
    } else {
        rng = document.body.createTextRange();
        rng.moveToElementText(element);
        rng.select();
    }
}

class RangeSelector {

    constructor(elements) {
        this.elements = elements;
        this.currentElement = null;
        this.prevIndex = -1;
        this.currentIndex = -1;
        this.nextIndex = -1;
    }

    nextRange() {
        if ((this.currentIndex + 1) >= this.elements.length) {
            this.setToStart();
        }
        this.currentElement = this.elements[++this.currentIndex];
        this.prevIndex = this.currentIndex - 1;
        this.nextIndex = this.currentIndex + 1;
        return this.currentElement;
    }

    prevRange() {
        if (this.prevIndex < 0) {
            this.setToEnd();
        }
        this.currentElement = this.elements[this.prevIndex];
        this.currentIndex = this.prevIndex;
        this.prevIndex--;
        this.nextIndex = this.currentIndex + 1;
        return this.currentElement;
    }

    setToStart() {
        this.prevIndex = -1;
        this.currentIndex = -1;
        this.nextIndex = -1;
    }

    setToEnd() {
        this.currentIndex = this.elements.length;
        this.prevIndex = this.currentIndex - 1;
        this.nextIndex = this.currentIndex + 1;
    }

}
