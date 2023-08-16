/*
Copyright 2007-2010 Chris Hibbert.  All rights reserved.
Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

This software is published under the terms of the MIT license, a copy
of which has been included with this distribution in the LICENSE file.
*/


// Functions referred to before being defined.  To make jslint stop complaining

/*global document, getDeleteOrdersInput  */

/*jslint browser: true */

//  use console.debug() with firebug for debugging.

///////////// modifying offers display

function findOffers() {
    return document.getElementById('offers');
}

function addRowToTable(table, text, color) {
    var row = document.createElement("tr");
    var cell = row.insertCell(0);
    cell.innerHTML = text;
    row.align = "center";
    row.bgColor = color;
    table.appendChild(row);
}

function addRowsIncreasing(table, prices, color) {
    var priceList = getPrices(prices);
    for (var i = 0 ; i < priceList.length ; i++ ) {
        addRowToTable(table, priceList[i], color);
    }
}

function addRowsDecreasing(table, prices, color) {
    var priceList = getPrices(prices);
    for (var j = priceList.length - 1 ; j >= 0 ; j-- ) {
        addRowToTable(table, priceList[j], color);
    }
}

function makeOffersTable(desc) {
    var newTable = document.createElement("table");
    newTable.className = "offersTable";
    addRowToTable(newTable, "<b>Offers to Sell</b>", "");
    addRowsIncreasing(newTable, desc.sell, "orange");
    addRowToTable(newTable, "&nbsp;", "");
    addRowsDecreasing(newTable, desc.buy, "limegreen");
    addRowToTable(newTable, "<b>Offers to Buy</b>", "");
    return newTable;
}

function replaceCurrentOffers(offers, desc) {
    if (!malleableDocument()) {
        return;
    }

    if (offers.hasChildNodes()) {
        offers.removeChild(offers.lastChild);
    }
    var newOffers = makeOffersTable(desc);
    offers.appendChild(newOffers);
}

function clear_offers(offers) {
    replaceCurrentOffers(offers, { sell: "", buy: ""});
}
