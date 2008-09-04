/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.reset;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessagingException;

/**
 * @author Iwein Fuld
 */
@SuppressWarnings("unchecked")
public class PollableFileSourceTests {

	private PollableFileSource pollableFileSource;

	private MessageCreator messageCreatorMock = createMock(MessageCreator.class);

	private File inputDirectoryMock = createMock(File.class);

	private File inputDirectory;

	private Message messageMock = createMock(Message.class);

	private FileFilter filterMock = createMock(FileFilter.class);

	private File fileMock = createMock(File.class);

	private Object[] allMocks = new Object[] { inputDirectoryMock, messageCreatorMock, messageMock, filterMock, fileMock };


	@Before
	public void initialize() throws Exception {
		// inputDirectory = File.createTempFile("inputDir", null);
		this.pollableFileSource = new PollableFileSource();
		pollableFileSource.setInputDirectory(inputDirectory);
		pollableFileSource.setMessageCreator(messageCreatorMock);
		pollableFileSource.setInputDirectory(inputDirectoryMock);
	}

	@Before
	public void setDefaultExpectations() {
		expect(fileMock.exists()).andReturn(true).anyTimes();
	}


	@Test
	public void straightProcess() throws Exception {
		reset(fileMock);
		expect(inputDirectoryMock.listFiles(isA(FileFilter.class))).andReturn(new File[] { fileMock });
		expect(messageCreatorMock.createMessage(isA(File.class))).andReturn(messageMock);
		expect(fileMock.exists()).andReturn(true);
		replay(allMocks);
		pollableFileSource.onSend(pollableFileSource.receive());
	}

	@Test(expected = MessagingException.class)
	public void requeueOnException() throws Exception {
		expect(inputDirectoryMock.listFiles(isA(FileFilter.class))).andReturn(new File[] { fileMock });
		expect(messageCreatorMock.createMessage(isA(File.class))).andThrow(new RuntimeException("just testing"));
		replay(allMocks);
		pollableFileSource.receive();
		fail();
	}

	@Test
	public void requeueOnFailure() throws Exception {
		expect(inputDirectoryMock.listFiles(isA(FileFilter.class))).andReturn(new File[] { fileMock });
		expect(inputDirectoryMock.listFiles(isA(FileFilter.class))).andReturn(new File[] {});
		expect(messageCreatorMock.createMessage(fileMock)).andReturn(messageMock).times(2);
		replay(allMocks);
		Message received = pollableFileSource.receive();
		pollableFileSource.onFailure(received, new RuntimeException("failed"));
		assertEquals(received, pollableFileSource.receive());
	}

	@Test
	public void noDuplication() throws Exception {
		expect(inputDirectoryMock.listFiles(isA(FileFilter.class))).andReturn(new File[] { fileMock });
		expect(inputDirectoryMock.listFiles(isA(FileFilter.class))).andReturn(new File[] {});
		expect(messageCreatorMock.createMessage(fileMock)).andReturn(messageMock);
		replay(allMocks);
		assertEquals(messageMock, pollableFileSource.receive());
		assertNull(pollableFileSource.receive());
	}
	
	@After
	public void verifyAll() {
		verify(allMocks);
	}

}
