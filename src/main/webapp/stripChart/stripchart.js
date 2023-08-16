/*
Copyright 2007-2009 Chris Hibbert.  All rights reserved.
Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

This software is published under the terms of the MIT license, a copy
of which has been included with this distribution in the LICENSE file.
*/

// Functions referred to before being defined.  To make jslint stop complaining

/*global document, getPrices, getNamedIFrame, updateStyle, makeLabelDiv, malleableDocument, addLabelCell */

/*jslint browser: true */

var chartMaker = function chartMaker() {
    var that = {};

    var chartHeight = 400;
    var chartWidth = 400;
    var maxPrice = 100;
    var minPrice = 0;

    var dotSpacing = chartHeight / maxPrice;
    var dotHeight = 6;
    var dotWidth = 6;  // repeated in chart.style.css as: .chart li width
    var colSpace = 2;

    var majorUnit = 5;
    var minorUnit = 0;

    /////////////////  accessors

    that.getHeight = function() { return chartHeight; };
    that.getWidth = function() { return chartWidth; };
    that.getMaxPrice = function() { return maxPrice; };
    that.getMinPrice = function() { return minPrice; };

    that.toString = function() {
        return "chart " + chartHeight + " x " + chartWidth;
    };

    ///////////// setting up the chart and background

    function makeHBarDiv(y) {
        var newdiv = document.createElement("div");
        newdiv.className = "hBar";
        updateStyle(newdiv, {width: chartWidth, top: y});
        return newdiv;
    }

    function makeMinorHBarDiv(y) {
        var newdiv = document.createElement("div");
        newdiv.className = "minorBar";
        updateStyle(newdiv, {width: chartWidth, top: y});
        return newdiv;
    }

    function makeMajorHBarDiv(y) {
        var newdiv = document.createElement("div");
        newdiv.className = "majorBar";
        updateStyle(newdiv, {width: chartWidth, top: y});
        return newdiv;
    }

    /////   Boundaries class to manage upper and lower limits

    function Boundaries(min, max) {
        this.min = 1 * min;
        this.max = 1 * max;
    }

    Boundaries.prototype.update = function(position) {
        var pos = 1 * position;
        if (pos >= maxPrice || pos <= minPrice) {
            return;
        }
        if (pos < this.min) {
            this.min = position;
        }
        if (pos > this.max) {
            this.max = position;
        }
    };

    Boundaries.prototype.notAtEdges = function() {
        return this.min != minPrice && this.min != maxPrice &&
               this.max != minPrice && this.max != maxPrice;
    };

    function defaultBoundaries() {
        return new Boundaries(maxPrice, minPrice);
    }

    /////   Candle diagram

    function absoluteVPos(yCoord) {
        return chartHeight - (yCoord * dotSpacing);
    }

    function absoluteHPos(xCoord) {
        var abs = xCoord * (dotWidth + colSpace);
        return abs;
    }

    function makeCandlestick(x, bounds) {
        if (bounds.notAtEdges()) {
            var newspan = document.createElement("span");
            newspan.className = "candleStick";
            updateStyle(newspan,
                {
                    width: dotWidth,
                    top: absoluteVPos(bounds.max),
                    left: absoluteHPos(x),
                    height: chartHeight - absoluteVPos(bounds.max - bounds.min)
                }  );
            return newspan;
        }
        return null;
    }

    function addListElements(list, prices, className, xPos, bounds) {
        if (null !== prices) {
            for (var i = 0 ; i < prices.length ; i++ ) {
                var height = prices[i];
                var listElement = document.createElement("li");
                listElement.className = className;
                bounds.update(height);
                var style = { top: absoluteVPos(height) - dotHeight/2,
                              left: absoluteHPos(xPos) };
                updateStyle(listElement, style);
                list.appendChild(listElement);
            }
        }
    }

    // A column in the stripchart is a UL with an LI for each price, plus
    // a candlestick, which consists of a styled span.
    function buildColumn(columnDesc, xPos) {
        var newList = document.createElement("ul");
        newList.className = "column";
        var buy =    getPrices(columnDesc.buy);
        var sell =   getPrices(columnDesc.sell);
        var traded = [];
        if(columnDesc.traded) {
            traded = getPrices(columnDesc.traded.quant);
        }
        var sellBounds = defaultBoundaries();
        var ignoreTradeBounds = defaultBoundaries();
        var buyBounds = defaultBoundaries();
        addListElements(newList, sell,  "sell",  xPos, sellBounds);
        addListElements(newList, traded, "traded", xPos, ignoreTradeBounds);
        addListElements(newList, buy,   "buy",   xPos, buyBounds);
        var candleBounds = new Boundaries(buyBounds.max, sellBounds.min);
        var candle = makeCandlestick(xPos, candleBounds);
        if (null !== candle) {
            newList.appendChild(candle);
        }
        return newList;
    }

    function repositionColumn(column, xPos) {
        if (column.firstChild === null) {
            return;
        }
        var absoluteX = absoluteHPos(xPos);

        for (var elem = column.firstChild ; elem !== null ; elem = elem.nextSibling) {
            updateStyle(elem, { left: absoluteX });
        }
    }

    function insertOrReplaceColumn(columnDesc, insert) {
        if (!malleableDocument()) {
            return;
        }
        var chart = findChart();
        var columns = chart.getElementsByTagName('ul');

        if (0 === columns.length) {
            chart.appendChild(buildColumn(columnDesc, 0));
            return;
        }

        var maxColumn = Math.round(chartWidth / (dotWidth + colSpace));
        var lastColumn = columns.length - 1;

        if (lastColumn >= maxColumn - 1) {
            for (var i = 1 ; i <= lastColumn ; i++) {
                repositionColumn(columns[i], i - 1);
            }
            chart.removeChild(columns[0]);
            lastColumn -= 1;
        }

        var liveColumn = columns[lastColumn];
        var newColumn = buildColumn(columnDesc, lastColumn);

        if (insert) {
            repositionColumn(liveColumn, lastColumn + 1);
            chart.insertBefore(newColumn, liveColumn);
        } else {
            chart.replaceChild(newColumn, liveColumn);
        }
    }

    //////////////////////  Scaling Charts and labels

    function setMaxPrice(p) {
        maxPrice = p;
        dotSpacing = chartHeight / maxPrice;
        that.redrawChart(findChart());
    }

    function makeLabelDiv(top, marker) {
        var div = document.createElement("div");
        updateStyle(div, {position: "absolute", top: top});
        div.innerHTML = marker;
        return div;
    }

    function addLabelCell(div, marker) {
        var unit = chartHeight / maxPrice;
        var top = chartHeight - (unit * marker);
        div.appendChild(makeLabelDiv(top, marker));
    }

    function makeTopLabelDiv() {
        var div = document.createElement("div");
        updateStyle(div, {position: "relative", top: -208});
        for (var i = majorUnit; i < maxPrice ; i += majorUnit) {
            addLabelCell(div, i);
        }
        return div;
    }

    function updateLabels() {
        var doc;
        var chartFrame = getNamedIFrame('stripchartframe');
        if (chartFrame === null) {
            doc = document;
        } else {
            doc = chartFrame.document;
        }

        var leftLabels = doc.getElementById("leftLabels"); // has a single div as a child
        var leftTopDiv = leftLabels.childNodes[0];
        var rightLabels = doc.getElementById("rightLabels");
        var rightTopDiv = rightLabels.childNodes[0];

        var newLeftDiv = makeTopLabelDiv();
        var rightDiv = makeTopLabelDiv();

        leftLabels.replaceChild(newLeftDiv, leftTopDiv);
        rightLabels.replaceChild(rightDiv, rightTopDiv);
    }

    /////////////////  public methods

    that.addChartBackground = function addChartBackground(chart) {
        var scale = chartHeight / maxPrice;
        chart.style.width = chartWidth;
        chart.style.height = chartHeight;

        for (var i = chart.childNodes.length - 1; i >= 0; i--) {
            var child = chart.childNodes[i];
            if (child.nodeName == "DIV") {
                chart.removeChild(child);
            }
        }

        if (minorUnit === 0) {
            for (var k = majorUnit; k < maxPrice; k = majorUnit + k) {
                chart.appendChild(makeHBarDiv(chartHeight - (k * scale)));
            }
        } else {
            for (var y = minorUnit; y < maxPrice; y = minorUnit + y) {
                if (y % majorUnit !== 0) {
                    chart.appendChild(makeMinorHBarDiv(chartHeight - (y * scale)));
                }
            }
            for (var j = majorUnit; j < maxPrice; j = majorUnit + j) {
                chart.appendChild(makeMajorHBarDiv(chartHeight - (j * scale)));
            }
        }
    }

    that.updateScale = function updateScale(maxPriceFromServer) {
        var doc;
        var chartFrame = getNamedIFrame('subFrame');
        if (chartFrame === null) {
            doc = document;
        } else {
            doc = chartFrame.document;
        }
        var scaleDiv = doc.getElementById("scale");
        var minorDiv = doc.getElementById("minorUnit");
        var majorDiv = doc.getElementById("majorUnit");

        var newScale = 100;
        if(maxPriceFromServer != null) {
            newScale = maxPriceFromServer;
        } else if (scaleDiv != null) {
            newScale = (1 * scaleDiv.className)
        }

        var newMinorUnit = (minorDiv === null) ? 0 : (1 * minorDiv.className);
        var newMajorUnit = (majorDiv === null) ? newScale / 10 : (1 * majorDiv.className);

        minorUnit = newMinorUnit;
        majorUnit = newMajorUnit;
        setMaxPrice(newScale);
    };

    that.redrawChart = function redrawChart(chart) {
        that.addChartBackground(chart);
        updateLabels();
    };

    that.addHistoricalColumn = function addHistoricalColumn(columnDesc) {
        if (columnDesc.tradeType === "net.commerce.zocalo.ajax.events.LimitTrade") {
            insertOrReplaceColumn(columnDesc, true);
        }
    };

    that.replaceLiveColumn = function replaceLiveColumn(columnDesc) {
        insertOrReplaceColumn(columnDesc, false);
    };

    that.clear_chart = function clear_chart() {
        if (!malleableDocument()) {
            return;
        }

        var chart = findChart();
        var columns = chart.getElementsByTagName('ul');
        for (var index = columns.length - 1 ; index >= 0 ; index -- ) {
            chart.removeChild(columns[index]);
        }
    };

    // used in tests
    that.getRowLabel = function getRowLabel(col, row) {
        var labels;
        if (col === 0) {
            labels = document.getElementById("leftLabels");
        } else {
            labels = document.getElementById("rightLabels");
        }
        var div = labels.childNodes[0];
        var len = div.childNodes.length;
        var label;
        var count = (2 * maxPrice / majorUnit) - 1;
        if (count == len) {
            label = div.childNodes[count - (2 * row)].innerHTML;
        } else {
            label = div.childNodes[len - row].innerHTML;
        }
        return label;
    };

    return that;
};
