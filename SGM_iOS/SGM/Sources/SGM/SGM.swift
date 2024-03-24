//
//  SGM.swift
//  SGM_iOS
//
//  Created by illo on 3/23/24.
//

import CoreBluetooth
import Foundation
import UltraliteSDK

public class SGM: NSObject, CBCentralManagerDelegate {
    
    public static let shared = SGM()
    
    private var isActive: Bool = false
    
    private var displayTimeout: Int = 60
    private var maximumNumTaps: Int = 1
    
    let ultraliteLeftSidePixelBuffer = 40
    
    private var isConnectedListener: BondListener<Bool>?
    
    private var textHandle: Int?
    private var tapTextHandle: Int?
    
    var centralManager: CBCentralManager?
    
    var scanCallback: ((CBPeripheral) -> Void)?
    
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        print("CentralManager is initialized")

        switch central.state{
        case CBManagerState.unauthorized:
            print("The app is not authorized to use Bluetooth low energy.")
        case CBManagerState.poweredOff:
            print("Bluetooth is currently powered off.")
        case CBManagerState.poweredOn:
            print("Bluetooth is currently powered on and available to use.")
            let scanResult: UltraliteManager.BluetoothScanResult = UltraliteManager.shared.startScan(callback: { (peripheral: CBPeripheral) -> () in
                if let callback = self.scanCallback {
                    callback(peripheral)
                }
            })
            print("scanResult: \(scanResult)")
        default:break
        }
    }
    
    func startControl() {
        if !isActive {
            let startControlGranted: Bool? = UltraliteManager.shared.currentDevice?.requestControl(layout: .canvas, timeout: displayTimeout, hideStatusBar: true)
            print("startControlGranted: \(startControlGranted ?? false)")
        }
    }
    
    public func scan(callback: @escaping ((CBPeripheral) -> Void)) {
        scanCallback = callback
        if let device = UltraliteManager.shared.currentDevice, device.isConnected.value == true {
            // we have a device and it's connected
            print("we have a device and it's connected")
        } else if UltraliteManager.shared.currentDevice != nil {
            // we have a device but it isn't connected
            print("we have a device but it isn't connected")
        } else {
            print("No device")
            centralManager = CBCentralManager.init(delegate: self, queue: nil, options: [:])
        }
    }
    
    public func link(peripheral: CBPeripheral) {
        UltraliteManager.shared.link(device: peripheral, callback: { (ultralite: Ultralite?) -> Void in
            print("ultralite?.getName(): \(String(describing: ultralite?.getName()))")
        })
    }
    
    public func displayReferenceCardSimple(title: String, body: String) {
        guard let device = UltraliteManager.shared.currentDevice else {
            return
        }
        
        startControl()
        
        device.canvas.clear()
        
        _ = device.canvas.createText(text: title, textAlignment: .auto, textColor: .white, anchor: .topLeft, xOffset: ultraliteLeftSidePixelBuffer, yOffset: 120, isVisible: true, width: 640 - ultraliteLeftSidePixelBuffer, height: -1, wrapMode: .wrap)
        _ = device.canvas.createText(text: body, textAlignment: .auto, textColor: .white, anchor: .midLeft, xOffset: ultraliteLeftSidePixelBuffer, yOffset: 0, isVisible: true, width: 640 - ultraliteLeftSidePixelBuffer, height: -1, wrapMode: .wrap)
        
        device.canvas.commit()
    }
    
}

