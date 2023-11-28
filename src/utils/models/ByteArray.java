package utils.models;

import java.util.Arrays;

public class ByteArray {
	
	private byte[] arr;

	public ByteArray(byte[] array) {
		arr = array;
	}
	
	public byte[] getArr() {
		return arr;
	}
	
	@Override
    public boolean equals(Object obj) {
        return obj instanceof ByteArray && Arrays.equals(arr, ((ByteArray)obj).getArr());
    }
	
    @Override
    public int hashCode() {
        return Arrays.hashCode(arr);
    }

}
