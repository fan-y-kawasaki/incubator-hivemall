/*
 * Hivemall: Hive scalable Machine Learning Library
 *
 * Copyright (C) 2015 Makoto YUI
 * Copyright (C) 2013-2015 National Institute of Advanced Industrial Science and Technology (AIST)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hivemall.fm;

import hivemall.fm.FieldAwareFactorizationMachineModel.Entry;
import hivemall.utils.collections.OpenHashTable;
import hivemall.utils.io.IOUtils;
import hivemall.utils.lang.ObjectUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FFMPredictionModel implements Externalizable {

    private OpenHashTable<String, Entry> _map;
    private double _w0;
    private int _factors;

    public FFMPredictionModel() {}// for Externalizable

    public FFMPredictionModel(@Nonnull OpenHashTable<String, Entry> map, double w0, int factor) {
        this._map = map;
        this._w0 = w0;
        this._factors = factor;
    }

    public int getNumFactors() {
        return _factors;
    }

    public double getW0() {
        return _w0;
    }

    public float getW1(@Nonnull Feature e) {
        String xi = e.getFeature();
        Entry entry = _map.get(xi);
        if (entry == null) {
            return 0.f;
        }
        return entry.W;
    }

    @Nullable
    public float[] getV(@Nonnull Feature x, @Nonnull String field) {
        String j = StringFeature.getFeatureOfField(x, field);
        Entry entry = _map.get(j);
        if (entry == null) {
            return null;
        }
        return entry.Vf;
    }

    @Override
    public void writeExternal(@Nonnull ObjectOutput out) throws IOException {
        out.writeDouble(_w0);
        out.writeInt(_factors);

        int used = _map.size();
        out.writeInt(used);
        final Object[] keys = _map.getKeys();
        final Object[] values = _map.getValues();
        final byte[] status = _map.getStates();
        final int size = keys.length;
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            String key = (String) keys[i];
            if (key == null) {
                out.writeBoolean(true); // is null
                continue;
            } else {
                out.writeBoolean(false); // is null      
            }
            IOUtils.writeString(key, out);
            Entry v = (Entry) values[i];
            out.writeFloat(v.W);
            IOUtils.writeFloats(v.Vf, out);
            out.writeByte(status[i]);
        }
    }

    @Override
    public void readExternal(@Nonnull ObjectInput in) throws IOException, ClassNotFoundException {
        this._w0 = in.readDouble();
        this._factors = in.readInt();

        int used = in.readInt();
        final int size = in.readInt();
        final String[] keys = new String[size];
        final Entry[] values = new Entry[size];
        final byte[] states = new byte[size];
        for (int i = 0; i < size; i++) {
            if (in.readBoolean()) {// is null
                continue;
            }
            keys[i] = IOUtils.readString(in);
            float W = in.readFloat();
            float[] Vf = IOUtils.readFloats(in);
            values[i] = new Entry(W, Vf);
            states[i] = in.readByte();
        }

        this._map = new OpenHashTable<String, Entry>(keys, values, states, used);
    }

    public byte[] serialize() throws IOException {
        return ObjectUtils.toCompressedBytes(this);
    }

    public static FFMPredictionModel deserialize(@Nonnull final byte[] serializedObj)
            throws ClassNotFoundException, IOException {
        FFMPredictionModel model = new FFMPredictionModel();
        ObjectUtils.readCompressedObject(serializedObj, model);
        return model;
    }

}
