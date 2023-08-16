#!/usr/bin/env perl

#     Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
#    This file is published under the terms of the MIT license, a copy of
#    which has been included with this distribution in the LICENSE file.

package Zocalo;

use strict;
use ProcessLogs;

my $filename = @ARGV[0];
my $DELIM = ",";
open(LOGFILE, $filename);
my @lines=<LOGFILE>;

sub printAll {
	my($time, $event, $actor, $action, $quant, $price, $buyer, $seller, $n, $o, $p, $q, $r, $s) = @_;
	ProcessLogs::printExpCsv($filename, $time, $event, $actor, $action, $quant, $price, $buyer, $seller, $n, $o, $p, $q, $r, $s);
}

sub printTitles {
	print "SessionID${DELIM}Time${DELIM}Event${DELIM}Round${DELIM}Actor${DELIM}Action${DELIM}Quant${DELIM}Price${DELIM}";
	print "Buyer${DELIM}Seller${DELIM}BestBid${DELIM}BestAsk${DELIM}Role${DELIM}";
	print "Extra 1${DELIM}Extra 2${DELIM}Extra 3${DELIM}";
	print "Extra 4${DELIM}Extra 5${DELIM}Extra 6\n";
}

local *ProcessLogs::printAll =  'Zocalo::printAll';
local *ProcessLogs::printTitles =  'Zocalo::printTitles';

foreach(@lines) {
	processLines();
}

close (LOGFILE);

