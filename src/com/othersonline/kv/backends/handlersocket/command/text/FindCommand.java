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
package com.othersonline.kv.backends.handlersocket.command.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.othersonline.kv.backends.handlersocket.FindOperator;
import com.othersonline.kv.backends.handlersocket.impl.ResultSetImpl;
import com.othersonline.kv.backends.handlersocket.network.buffer.IoBuffer;
import com.othersonline.kv.backends.handlersocket.network.hs.HandlerSocketSession;

/**
 * A find command
 * 
 * @author dennis
 * @date 2010-11-27
 * 
 * @author stingleff
 * @date 2011-01-11
 */
public class FindCommand extends AbstractCommand {
	private final String id;
	private final String operator;
	private final String[] keys;
	private final int limit;
	private final int offset;
	private final String[] fieldList;

	public FindCommand(String id, FindOperator operator, String[] keys,
			int limit, int offset, String[] fieldList) {
		super();
		this.id = id;
		this.operator = operator.getValue();
		this.keys = keys;
		this.limit = limit;
		this.offset = offset;
		this.fieldList = fieldList;
	}

	@Override
	protected void onDone() {
		if (this.result == null) {
			this.result = new ResultSetImpl(Collections
					.<List<byte[]>> emptyList(), this.fieldList, this.encoding);
		}
	}

	@Override
	protected void decodeBody(HandlerSocketSession session, byte[] data,
			int index) {
		List<List<byte[]>> rows = new ArrayList<List<byte[]>>(this
				.getNumColumns());
		int offset = 0;
		int cols = this.fieldList.length;
		List<byte[]> currentRow = new ArrayList<byte[]>(this.fieldList.length);
		int currentCols = 0;
		int resultByteSize = 0;
		for (int i = 0; i < data.length; i++) {
			if (data[i] == TOKEN_SEPARATOR || data[i] == COMMAND_TERMINATE) {
				// previous decoding was:
				// byte[] colData = new byte[i - offset];
				// System.arraycopy(data, offset, colData, 0, colData.length);
				// this is modified to follow protocol:
				// "A character in the range [0x00 - 0x0f] is prefixed by 0x01 and shifted by 0x40"
				byte[] colData = new byte[resultByteSize];
				boolean shift = false;
				int colDataIndex = 0;
				for (int j = offset; j < (i - offset); ++j) {
					byte b = data[j];
					if (b == 0x01)
						shift = true;
					else {
						colData[colDataIndex] = (shift) ? (byte) (b - 0x40) : b;
						shift = false;
						++colDataIndex;
					}
				}
				currentRow.add(colData);
				currentCols++;
				offset = i + 1;
				if (currentCols == cols) {
					currentCols = 0;
					rows.add(currentRow);
					currentRow = new ArrayList<byte[]>(this.fieldList.length);
				}
			} else if (data[i] != 0x01) {
				++resultByteSize;
			}
		}
		if (!currentRow.isEmpty()) {
			rows.add(currentRow);
		}
		this.result = new ResultSetImpl(rows, this.fieldList, this.encoding);
	}

	public void encode() {
		IoBuffer buf = IoBuffer.allocate(this.id.length() + 1
				+ this.operator.length() + 1 + this.length(this.keys)
				+ this.keys.length + 1 + 10);

		// id
		this.writeToken(buf, this.id);
		this.writeTokenSeparator(buf);
		// operator
		this.writeToken(buf, this.operator);
		this.writeTokenSeparator(buf);
		// value nums
		this.writeToken(buf, String.valueOf(this.keys.length));
		this.writeTokenSeparator(buf);
		for (String key : this.keys) {
			this.writeToken(buf, key);
			this.writeTokenSeparator(buf);
		}
		// limit
		this.writeToken(buf, String.valueOf(this.limit));
		this.writeTokenSeparator(buf);
		// offset
		this.writeToken(buf, String.valueOf(this.offset));
		this.writeCommandTerminate(buf);

		buf.flip();
		// System.out.println(Arrays.toString(buf.array()));
		this.buffer = buf;
	}

}
