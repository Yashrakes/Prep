package com.HLDLLD.structural.Bridge;


import com.HLDLLD.structural.Bridge.Device.Device;

public class AdvancedRemote extends BasicRemote{
    public AdvancedRemote(Device device) {
        //super(device);
        super.device = device;
    }

    public void mute() {
        System.out.println("Remote: mute");
        device.setVolume(0);
    }
}
