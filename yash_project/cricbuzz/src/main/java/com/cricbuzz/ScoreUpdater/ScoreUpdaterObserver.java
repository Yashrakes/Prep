package com.cricbuzz.ScoreUpdater;

import com.cricbuzz.Inning.BallDetails;

public interface ScoreUpdaterObserver {

    public void update(BallDetails ballDetails);
}


