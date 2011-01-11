/**
 *Copyright [2010-2011] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.othersonline.kv.backends.handlersocket.network.hs.codec;

import com.othersonline.kv.backends.handlersocket.Command;
import com.othersonline.kv.backends.handlersocket.network.buffer.IoBuffer;
import com.othersonline.kv.backends.handlersocket.network.core.Session;
import com.othersonline.kv.backends.handlersocket.network.core.CodecFactory.Encoder;

/**
 * HandlerSocket protocol encoder
 * 
 * @author dennis
 * 
 */
public class HandlerSocketEncoder implements Encoder {

	public IoBuffer encode(Object message, Session session) {
		return ((Command) message).getIoBuffer();
	}

}
