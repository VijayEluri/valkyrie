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

import com.othersonline.kv.backends.handlersocket.Command;
import com.othersonline.kv.backends.handlersocket.CommandFactory;
import com.othersonline.kv.backends.handlersocket.FindOperator;
import com.othersonline.kv.backends.handlersocket.Protocol;

/**
 * HandlerSocket text protocol command factory
 * 
 * @author dennis
 * @date 2010-11-27
 */
public class TextCommandFactory implements CommandFactory {

	public Protocol getProtocol() {
		return Protocol.Text;
	}

	public Command createOpenIndexCommand(String id, String db,
			String tableName, String indexName, String[] fieldList) {
		return new OpenIndexCommand(id, db, tableName, indexName, fieldList);
	}

	public Command createInsertCommand(String id, byte[][] values) {
		return new InsertCommand(id, values);
	}

	public Command createFindCommand(String id, FindOperator operator,
			String[] keys, int limit, int offset, String[] fieldList) {
		return new FindCommand(id, operator, keys, limit, offset, fieldList);
	}

	public Command createUpdateCommand(String id, FindOperator operator,
			String[] keys, byte[][] values, int limit, int offset) {
		return new ModifyCommand(id, operator, keys, values, limit, offset,
				AbstractCommand.OPERATOR_UPDATE);
	}

	public Command createDeleteCommand(String id, FindOperator operator,
			String[] keys, int limit, int offset) {
		return new ModifyCommand(id, operator, keys, new byte[1][keys.length],
				limit, offset, AbstractCommand.OPERATOR_DELETE);
	}

}
