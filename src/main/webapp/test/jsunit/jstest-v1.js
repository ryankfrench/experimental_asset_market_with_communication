/* jstest: Simple JavaScript testing library, v1 */

/* Copyright Kragen Sitaker, 2005. Licensed under GNU GPL.  See end of
 * file for details.*/

/* QUICK USE INSTRUCTIONS:

   1. Include this file in your HTML:
       <script src="http://pobox.com/~kragen/sw/jstest-v1.js"></script>

   2. Add some tests --- look for "tests = " in this file for more examples:
       <script type="text/javascript">
       tests = { basic_arithmetic: function() { assert_equal(3 + 4, 7) } };
       </script>

   3. Add an onLoad that calls "barebones_run_tests":
       <body onLoad="barebones_run_tests()">

   4. Load the page in Mozilla.  (Seems broken in Konqueror; dunno why.)

   This library uses the following function and variable names.
   Redefining them yourself will change its operation:

       textarea_output, test_output, set_test_output, current_test,
       current_result, run_tests, assert, eql_array, eql, eql_object,
       property_names, make_set, all_in_set, assert_equal, barebones_run_tests

*/

/* SLIGHTLY MORE DETAILED DOCS:

tests -- you define this to point at your tests.  See example near
    bottom of this file.

Internal Testing Variables:
test_output -- the variable holding the current output object
current_test -- the name of the currently-executing test
current_result -- the results of the currently-executing test
    XXX hide these

Test-Suite Running Functions:
barebones_run_tests -- adds a textarea to the document and runs the test suite
    at window.tests in it
textarea_output -- constructs an output object that outputs to a given textarea.
set_test_output -- redirects test output to a new place
run_tests -- runs a particular suite of tests

Test-Suite Construction Functions:
assert(condition, msg) -- fails a test if a condition is not true; msg optional
assert_equal(a, b) -- fails a test if two objects are not structurally the same
    XXX debug message?

Utility Functions:
eql(a, b) -- returns true if two objects are structurally the same
property_names -- returns an array of the names "for (x in obj)" iterates over
make_set -- turns an array of strings into an object for quick membership tests
all_in_set(aset, alist) -- returns true if all the items in alist are "in" aset
eql_object -- helper function for eql XXX hide
eql_array -- ditto XXX hide

Methods Test Objects Should Implement (textarea_output's return value,
   for example):
pass(name) -- test called "name" has passed
fail(name, why) -- test called "name" failed, for reason "why"
debugmsg(msg) -- someone can't figure out what's going on and wants printf
   XXX this needs fixing as there's no way to call it

*/

function textarea_output(out_textarea) {
    return {
        debugmsg: function(str) { out_textarea.value += "<<" + str + ">>" },
        pass: function(name) { out_textarea.value += "passed: " + name + "\n" },
        fail: function(name, why) { out_textarea.value += "FAILED: " + name + " (" + why + ")\n" }
    };
}

test_output = null;
function set_test_output(out_thingy) { test_output = out_thingy }

// If JavaScript were really prototype-oriented, we'd create an object
// inheriting from "suite" and set its output to test_output here,
// instead of using global variables like this.

current_test = null;
current_result = null;
function run_tests(suite) {
    for (current_test in suite) {
        var raised_exception = false;
        current_result = null;
        try {
            suite[current_test]();
        } catch (e) {
            raised_exception = e;
        }
        if (raised_exception) {
            test_output.fail(current_test, "exception: " + raised_exception);
        } else if (current_result == null) {
            test_output.fail(current_test, "no assertions");
        } else if (current_result) {
            test_output.pass(current_test);
        }
    }
}

function assert(truth, why) {
    if (truth) {
        if (current_result == null) current_result = true;
    } else {
        if (why == null) why = "assertion failed";
        test_output.fail(current_test, why);
        current_result = false;
    }
}

function eql_array(a, b) {
    if (a.length != b.length) return false;
    for (var i = 0; i < a.length; i++) {
        if (!eql(a[i], b[i])) return false;
    }
    return true;
}

function property_names(obj) {
    // I can't believe this isn't a primitive in JavaScript
    var rv = [];
    for (var i in obj) rv.push(i);
    return rv;
}

function make_set(array) {
    var rv = {};
    for (var i = 0; i < array.length; i++)
        rv[array[i]] = 1;
    return rv;
}

function all_in_set(set, items) {
    for (var i = 0; i < items.length; i++)
        if (!set[items[i]]) return false;
    return true;
}

function eql_object(a, b) {
    if (a.constructor != b.constructor) return false;
    var anames = property_names(a);
    var bnames = property_names(b);
    if (!all_in_set(make_set(anames), bnames)) return false;
    if (!all_in_set(make_set(bnames), anames)) return false;
    for (var i in anames) {
        var name = anames[i];
        if (!eql(a[name], b[name])) return false;
    }
    return true;
}

function eql(a, b) {
    if (a.constructor == Array && b.constructor == Array) {
        return eql_array(a, b);
    } else if (a.constructor == Number || a.constructor == String ||
               a.constructor == Function) {
        return (a == b);
    } else {
        return eql_object(a, b);
    }
}

function assert_equal(a, b) {
    assert(eql(a, b), a.toSource() + " != " + b.toSource());
}

tests = {
    simple_pass: function() { assert(3 + 4 == 7) },
    double_pass: function() { assert(3 + 4 == 7); assert(3 + 4 == 7) },

    // these few tests fail, in order to test testing
    /*
    simple_fail: function() { assert(2 + 2 == 5) },
    fail_then_pass: function() { assert(2 + 2 == 5); assert(3 + 4 == 7) },
    simple_notests: function() { return; assert(3 + 4 == 7) },
    */

    simple_equal: function() { assert(eql(1, 1)) },
    simple_unequal: function() { assert(!eql(1, 2)) },
    empty_list_equal: function() { assert(eql([], [])) },
    length_mismatch: function() { assert(!eql([1], [])) },
    item_mismatch: function() { assert(!eql([1], [2])) },
    nonempty_list_equal: function() { assert(eql([1], [1])) },

    nested_list_equal: function() { assert(eql([[[3]]], [[[3]]])) },
    nested_list_unequal: function() { assert(!eql([[[3]]], [[[4]]])) },

    string_equal: function() { assert(eql("yes", "yes")) },
    string_unequal: function() { assert(!eql("nes", "yes")) },

    property_names: function() {
        assert_equal(property_names({}), []);
        assert_equal(property_names({x:1}), ['x']);
        assert_equal(property_names({x:[]}), ['x']);
    },

    bare_object_equal: function() { assert(eql({}, {})) },
    bare_object_assert_equal: function() { assert_equal({}, {}) },
    bare_object_unequal: function() { assert(!eql({x:[]}, {})) },
    nonempty_object_equal: function() { assert(eql({x:[]}, {x:[]})) },

    constructors: function() {
        function Foo() { };
        function Boo() { };
        var x = new Foo();
        var y = new Boo();
        var yy = new Boo();
        assert_equal(y, y);
        assert_equal(y, yy);
        assert(!eql(x, y));
    },
    mismatched_values: function() { assert(!eql({x:1}, {x:2})) },
    mismatched_orders: function() { assert_equal({a:1,b:1}, {b:1,a:1}) },
    matched_functions: function() { var x = function(){}; assert_equal(x, x) },
    mismatched_functions: function() { assert(!eql(function(){}, function(){1})) },
    // XXX: handling of null and undefined
}

function barebones_run_tests() {
    var textarea = document.createElement('textarea');
    textarea.rows = 60;
    textarea.cols = 80;
    textarea.value = "Barebones test results at " + new Date() + ":\n";
    document.body.appendChild(textarea);
    set_test_output(textarea_output(textarea));
    run_tests(window.tests);
}

/* jstest -- a JavaScript unit testing program
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * Kragen Sitaker does not consider programs tested using this program
 * to be derivative works thereof, and therefore proprietary software
 * may be tested with it.  However, the test suites themselves might
 * be derivative works of this program.
 */

// -->
