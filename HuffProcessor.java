import java.util.*;


public class HuffProcessor implements Processor{
	
	int[] occurences = new int[ALPH_SIZE];
	String[] paths = new String[ALPH_SIZE + 1];

	@Override
	public void compress(BitInputStream in, BitOutputStream out) {
		// TODO Auto-generated method stub
		//1. count characters in file

		int readNumbers = in.readBits(BITS_PER_WORD);
		while(readNumbers != -1){
			occurences[readNumbers] ++;
			readNumbers = in.readBits(BITS_PER_WORD);

		}
		in.reset();

		//2. create huffman tree

		PriorityQueue<HuffNode> HuffNodes = new PriorityQueue<HuffNode>();
		for(int i = 0; i < ALPH_SIZE; i++){
			if(occurences[i] != 0){
				HuffNodes.add(new HuffNode(i, occurences[i]));
			}
		}
		HuffNodes.add(new HuffNode(PSEUDO_EOF, 0));

		while(HuffNodes.size()  > 1){
			HuffNode small1 = HuffNodes.poll();
			HuffNode small2 = HuffNodes.poll();     //pull two smallest nodes;
			HuffNodes.add(new HuffNode( -1, small1.weight() + small2.weight(), small1, small2));

		}
		
		extractCodes(HuffNodes.peek(), "");
		out.writeBits(BITS_PER_INT, HUFF_NUMBER);
		writeHeader(HuffNodes.peek(), out);
		
		int readBit = in.readBits(BITS_PER_WORD);
		while( readBit != -1){
			String code = paths[readBit];
			out.writeBits(code.length(), Integer.parseInt(code, 2));
			readBit = in.readBits(BITS_PER_WORD);
			
		}
	//6. write the pseudo-EDF, so you know when to stop decompressing
	//same thing but with pseudo-EOF instead of in.readBits
		
	
		String pseudo = paths[PSEUDO_EOF];
		out.writeBits(pseudo.length(), Integer.parseInt(pseudo, 2));

}
	//3. traverse tree and extract all the codes in the tree
	private void extractCodes(HuffNode current, String path){
		if(current.left() == null && current.right() == null){
			paths[current.value()] = path;
			return;
		}
		extractCodes(current.left(), path + 0);
		extractCodes(current.right(), path + 1);
	}
	
	//4. write the header
	private void writeHeader(HuffNode current, BitOutputStream out){
		if(current.left() == null && current.right() == null){
			out.writeBits(1, 1);
			out.writeBits(9, current.value());
			return;
		}
		out.writeBits(1, 0);
		writeHeader(current.left(), out);
		writeHeader(current.right(), out);
	}
	 //5. compress and write the body of the file
			
	@Override
	public void decompress(BitInputStream in, BitOutputStream out) {
		// TODO Auto-generated method stub
		//1. check for HUFF_NUMBER
		if (in.readBits(BITS_PER_INT) != HUFF_NUMBER){
			throw new HuffException("HuffException Error");
		}

		//3. parse body of compressed file
				
		HuffNode root = readHeader(in);
		HuffNode current = root;
		int readCurrent = in.readBits(1);

		while(readCurrent != -1){
			if(readCurrent == 1){
				current = current.right();
			}else{
				current = current.left();

			}
			if(current.left() == null && current.right() == null){
				if(current.value() == PSEUDO_EOF){
					return;
				}else{
					out.writeBits(8, current.value());
					current = root;
				}
			}
			readCurrent = in.readBits(1);
		}
		throw new HuffException("HuffException Error");
	}

	private HuffNode readHeader(BitInputStream in){
		if(in.readBits(1) == 0){
			HuffNode left = readHeader(in);
			HuffNode right = readHeader(in);
			return new HuffNode(-1, -1, left, right);
		}else{
			return new HuffNode(in.readBits(9), 0) ;
		}

	}


}