/*
 * Copyright (c) 2020. BoostTag E.I.R.L. Romell D.Z.
 * All rights reserved
 * porfile.romellfudi.com
 */
package com.romellfudi.fudinfc.util.sync

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.util.SparseArray
import com.romellfudi.fudinfc.util.constants.NfcType
import com.romellfudi.fudinfc.util.interfaces.NfcReadUtility
import java.nio.charset.Charset
import java.util.*

class NfcReadUtilityImpl : NfcReadUtility {
    /**
     * {@inheritDoc}
     */
    override fun readFromTagWithSparseArray(nfcDataIntent: Intent?): SparseArray<String?>? {
        val messages = nfcDataIntent!!.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        val resultMap: SparseArray<String?> =
            if (messages != null) SparseArray(messages.size) else SparseArray(0)
        if (messages == null) {
            return resultMap
        }
        for (message in messages) {
            for (record in (message as NdefMessage).records) {
                val type = retrieveTypeByte(record.payload)
                val i = resultMap[type.toInt()]
                if (i == null) {
                    resultMap.put(type.toInt(), parseAccordingToType(record))
                }
            }
        }
        return resultMap
    }

    /**
     * {@inheritDoc}
     */
    override fun readFromTagWithMap(nfcDataIntent: Intent?): Map<Byte?, String?>? {
        val resultMap: MutableMap<Byte?, String?> = HashMap()
        val sparseArray = readFromTagWithSparseArray(nfcDataIntent)
        for (i in 0 until sparseArray!!.size()) {
            resultMap[sparseArray.keyAt(i).toByte()] = sparseArray.valueAt(i)
        }
        return resultMap
    }

    /**
     * {@inheritDoc}
     */
    override fun retrieveMessageTypes(record: NdefMessage?): Iterator<Byte?>? {
        val list: MutableCollection<Byte> = ArrayList()
        for (ndefRecord in record!!.records) {
            list.add(retrieveTypeByte(ndefRecord.payload))
        }
        return list.iterator()
    }

    /**
     * {@inheritDoc}
     */
    override fun retrieveMessage(message: NdefMessage?): String? {
        return if (message!!.records[0] != null) parseAccordingToHeader(message.records[0].payload) else null
    }

    private fun retrieveTypeByte(payload: ByteArray): Byte {
        return if (payload.size > 0) {
            payload[0]
        } else -1
    }

    private fun parseAccordingToHeader(payload: ByteArray): String {
        return if (payload.size > 0) String(
            payload,
            1,
            payload.size - 1,
            Charset.forName("US-ASCII")
        ).trim { it <= ' ' } else ""
    }

    private fun parseAccordingToType(obj: NdefRecord): String {
        if (Arrays.equals(obj.type, NfcType.BLUETOOTH_AAR)) {
            val toConvert = obj.payload
            val result = StringBuilder()
            for (i in toConvert.size - 1 downTo 2) {
                val temp = toConvert[i]
                val tempString =
                    if (temp < 0) Integer.toHexString(temp + Byte.MAX_VALUE) else Integer.toHexString(
                        temp.toInt()
                    )
                result.append(if (tempString.length < 2) "0$tempString" else tempString)
                result.append(":")
            }
            return if (result.length != 0) result.substring(
                0,
                result.length - 1
            ) else result.toString()
        }
        return Uri.parse(parseAccordingToHeader(obj.payload)).toString()
    }

    companion object {
        private val TAG = NfcReadUtilityImpl::class.java.canonicalName
    }
}