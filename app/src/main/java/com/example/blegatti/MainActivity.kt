package com.example.blegatti

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothGatt: BluetoothGatt? = null
    private val deviceName = "WQ-87A023190998"
    private val deviceAddress = "08:3A:8D:BE:DF:A2"
    private val serviceUUID = "00001816-0000-1000-8000-00805f9b34fb"
    private val characteristicUUID = "00002a57-0000-1000-8000-00805f9b34fb"
    private val devices = mutableListOf<BLEDevice>()
    private val uniqueDeviceAddresses = mutableSetOf<String>()
    private val services = mutableListOf<BLEService>()
    private val characteristics = mutableListOf<BLECharacteristic>()

    private val REQUEST_BLUETOOTH_PERMISSIONS = 1
    private var isDeviceFound = false
    private lateinit var deviceAdapter: DeviceListAdapter
    private lateinit var serviceAdapter: ServiceListAdapter
    private lateinit var characteristicAdapter: CharacteristicListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val scanButton: Button = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            if (!bluetoothAdapter.isEnabled) {
                // Request to enable Bluetooth
                // (Note: Add the Bluetooth enabling code here)
            } else {
                if (checkAndRequestPermissions()) {
                    startScan()
                }
            }
        }

        val stopButton: Button = findViewById(R.id.stopScanButton)
        stopButton.setOnClickListener {
            stopScan()
        }

        deviceAdapter = DeviceListAdapter(this, devices, deviceAddress)
        val deviceListView: ListView = findViewById(R.id.deviceList)
        deviceListView.adapter = deviceAdapter

        deviceListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedDevice = devices[position]
            Toast.makeText(this, "Connecting to ${selectedDevice.name ?: "Unknown"}", Toast.LENGTH_SHORT).show()

            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(selectedDevice.address)
            connectToDevice(bluetoothDevice)
        }

        serviceAdapter = ServiceListAdapter(this, services)
        val serviceListView: ListView = findViewById(R.id.serviceList)
        serviceListView.adapter = serviceAdapter

        serviceListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedService = services[position]
            displayCharacteristics(selectedService)
        }

        characteristicAdapter = CharacteristicListAdapter(this, characteristics)
        val characteristicListView: ListView = findViewById(R.id.characteristicList)
        characteristicListView.adapter = characteristicAdapter

        characteristicListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedCharacteristic = characteristics[position]
            enableNotifications(selectedCharacteristic.uuid)
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        return if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startScan()
            } else {
                Log.e("BLE", "Permissions denied.")
            }
        }
    }

    private fun startScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_BLUETOOTH_PERMISSIONS)
            return
        }
        isDeviceFound = false
        devices.clear()
        uniqueDeviceAddresses.clear()
        deviceAdapter.notifyDataSetChanged()

        val scanner = bluetoothAdapter.bluetoothLeScanner
        scanner.startScan(scanCallback)
        Log.i("BLE", "Scan started.")

        // Stop scan after a timeout
        Handler().postDelayed({
            if (!isDeviceFound) {
                scanner.stopScan(scanCallback)
                Log.i("BLE", "Scan stopped after timeout.")
            }
        }, 10000) // Stop scan after 10 seconds
    }

    private fun stopScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_BLUETOOTH_PERMISSIONS)
            return
        }
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        Log.i("BLE", "Scan stopped.")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!isDeviceFound) {
                val device = result.device
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_PERMISSIONS)
                    return
                }
                val deviceAddress = device.address
                if (!uniqueDeviceAddresses.contains(deviceAddress)) {
                    uniqueDeviceAddresses.add(deviceAddress)
                    devices.add(BLEDevice(device.name, deviceAddress))
                    deviceAdapter.notifyDataSetChanged()

                    if (device.name == this@MainActivity.deviceName && device.address == this@MainActivity.deviceAddress) {
                        isDeviceFound = true
                        bluetoothAdapter.bluetoothLeScanner.stopScan(this)
                        connectToDevice(device) // Passing BluetoothDevice directly
                        Log.i("BLE", "Scan stopped after finding device: ${device.name}")
                    }
                }
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_PERMISSIONS)
            return
        }
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i("BLE", "Connected to GATT server.")
                runOnUiThread {
                    findViewById<TextView>(R.id.statusText).text = "Connected to GATT server."
                }
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_PERMISSIONS)
                    return
                }
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i("BLE", "Disconnected from GATT server.")
                runOnUiThread {
                    findViewById<TextView>(R.id.statusText).text = "Disconnected from GATT server."
                }
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                services.clear()
                gatt.services.forEach { service ->
                    services.add(BLEService(service.uuid))
                }
                runOnUiThread {
                    findViewById<ListView>(R.id.serviceList).visibility = View.VISIBLE
                    serviceAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLE", "MTU set to $mtu")
            } else {
                Log.w("BLE", "Failed to set MTU")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value.toString(Charsets.UTF_8)
            Log.i("BLE", "Received data: $data")
            runOnUiThread {
                findViewById<TextView>(R.id.statusText).text = "Received data: $data"
            }
        }
    }

    private fun displayCharacteristics(service: BLEService) {
        val gattService = bluetoothGatt?.getService(service.uuid)
        characteristics.clear()
        gattService?.characteristics?.forEach { characteristic ->
            characteristics.add(BLECharacteristic(characteristic.uuid))
        }
        runOnUiThread {
            findViewById<ListView>(R.id.characteristicList).visibility = View.VISIBLE
            characteristicAdapter.notifyDataSetChanged()
        }
    }

    private fun enableNotifications(characteristicUUID: UUID) {
        val characteristic = bluetoothGatt?.getService(serviceUUID.toUUID())?.getCharacteristic(characteristicUUID)
        if (characteristic != null) {
            enableNotifications(bluetoothGatt!!, characteristic)
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_PERMISSIONS)
            return
        }
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(characteristic.uuid)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    private fun String.toUUID() = java.util.UUID.fromString(this)
}
