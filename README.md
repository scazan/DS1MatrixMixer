# DS1MatrixMixer
An Ndef-oriented matrix mixer class for SC 3.7 using the Livid DS1 with a built-in EQ mode, mute, and master fader.


## Using the class:
```
(
	Ndef(\noiseSynth, {
		WhiteNoise.ar();
	});

	Ndef(\sinSynth, {
		SinOsc.ar(220);
	});

	Ndef(\soundIn0, {
		SoundIn.ar(0)!2; // if you want to directly monitor a sound input
	});

	// Any other Ndef synths you might want to use...
)
(
	// All pre-fader sends from the channels can be accessed via a pre-defined Ndef(\busN). For example:

	Ndef(\reverb, {
		var input = Ndef(\bus0).ar; // this means any channel that has its first knob turned up will be fed into this reverb.

		GVerb.ar(input);
	});

	Ndef(\trackingSynth, {
		var freq, hasFreq;
		var input = Ndef(\bus1).ar; // this means any channel that has its second knob turned up will be fed into this pitch tracker.

		# freq, hasFreq = Pitch.kr(input);

		SinOsc.ar(freq)!2;
	});
)

(
// Instantiate the matrix mixer using the names of the Ndefs that you want to use.
// (currently, you must fill the empty channels with an empty ndef name. In this case \nothing. I will fix this soon)
m = DS1MatrixMixer.new([
	\reverb,
	\trackingSynth,
	\noiseSynth,
	\sinSynth,
	\soundIn0,
	\nothing,
	\nothing,
	\nothing
]);
)
```
