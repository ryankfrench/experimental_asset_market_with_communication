use strict;
use warnings;
use List::Util qw(any first);
use Data::Dumper;

my @halfTrades;
push(@halfTrades, { buyer => "a1", price => "50" });
push(@halfTrades, { seller => "a2", price => "100" });
push(@halfTrades, { buyer => "a1", price => "75" });

print Dumper(\@halfTrades);	

my $type = "buyer";
my $price = "52";

my $match = first { exists($_->{$type}) and $_->{price} eq $price } @halfTrades;

if($match) {
	my $matchIdx = first { exists($halfTrades[$_]->{$type}) and $halfTrades[$_]->{price} eq $price } 0..$#halfTrades;
	print "Match Found at index: " . $matchIdx . "\n";
	print Dumper($match);
	splice(@halfTrades, $matchIdx, 1);	
} else {
	print "No match.\n";
}

print Dumper(\@halfTrades);	