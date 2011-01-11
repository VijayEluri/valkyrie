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

import com.othersonline.kv.backends.handlersocket.network.buffer.IoBuffer;

/**
 * Open index command
 * 
 * @author dennis
 * @date 2010-11-27
 */
public class OpenIndexCommand extends AbstractCommand {
	private final String id;
	private final String db;
	private final String tableName;
	private final String indexName;
	private final String[] fieldList;

	public OpenIndexCommand(String id, String db, String tableName,
			String indexName, String[] fieldList) {
		super();
		this.id = id;
		this.db = db;
		this.tableName = tableName;
		this.indexName = indexName;
		this.fieldList = fieldList;
	}

	public void encode() {
		IoBuffer buf = IoBuffer.allocate(2 + this.id.length() + 1
				+ this.db.length() + 1 + this.tableName.length() + 1
				+ this.indexName.length() + 1 + this.length(this.fieldList)
				+ this.fieldList.length);
		buf.setAutoExpand(true);

		// header
		this.writeToken(buf, OPERATOR_OPEN_INDEX);
		this.writeTokenSeparator(buf);
		// id
		this.writeToken(buf, this.id);
		this.writeTokenSeparator(buf);
		// db name
		this.writeToken(buf, this.db);
		this.writeTokenSeparator(buf);
		// tableName
		this.writeToken(buf, this.tableName);
		this.writeTokenSeparator(buf);
		// indexName
		this.writeToken(buf, this.indexName);
		this.writeTokenSeparator(buf);
		// field list
		this.writeToken(buf, join(this.fieldList));
		this.writeCommandTerminate(buf);

		buf.flip();

		this.buffer = buf;

	}

}
