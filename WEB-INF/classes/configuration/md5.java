/*

Copyright (C) 2010 Grameen Foundation
Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
*/
package configuration;
import java.security.*;
import java.math.*;

public class md5 {

	public static String getMD5Hash(String s) throws Exception
	{
		MessageDigest m=MessageDigest.getInstance("MD5");
		m.update(s.getBytes(),0,s.length());
		String hash=new BigInteger(1,m.digest()).toString();
		StringBuffer buffer=new StringBuffer();
		int intValue;
		for(int x=0;x<hash.length();x++)
		{
			int cursor=0;
			intValue=hash.charAt(x);
			String binaryChar=new String(Integer.toBinaryString(hash.charAt(x)));
			for(int i=0;i<binaryChar.length();i++)
			{
				if(binaryChar.charAt(i)=='1')
				{
					cursor++;
				}
			}
			if((cursor%2)>0)
			{
				intValue+=128;
			}
			buffer.append(Integer.toHexString(intValue)+"");
		}
		return buffer.toString();
	}
}
