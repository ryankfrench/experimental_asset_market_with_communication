/*
Copyright 2007-2010 Chris Hibbert.  All rights reserved.
Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

This software is published under the terms of the MIT license, a copy
of which has been included with this distribution in the LICENSE file.
*/

function setJudgeDisabled(flag) {
    updateDisableFlagInForm('judgeScoringForm', 'priceEstimate', flag); // works for simple form
    dojo.byId('priceEstimate').setAttribute("disabled", flag);         // works for digit form
}

function resetJudgeEstimate(value) {
    dojo.byId('priceEstimate').setValue(value)
    updateValueInForm('judgeScoringForm', 'priceEstimate', value);
}
