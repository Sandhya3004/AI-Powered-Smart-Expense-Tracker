import { useState, useRef } from 'react';
import { useToast } from '@/hooks/use-toast';
import { api } from '@/api/api';
import { Mic, MicOff, Loader2, CheckCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

const VoiceInputButton = ({ onExpenseCreated }) => {
  const [isRecording, setIsRecording] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [audioURL, setAudioURL] = useState('');
  const mediaRecorderRef = useRef(null);
  const audioChunksRef = useRef([]);
  const { toast } = useToast();

  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      
      const mediaRecorder = new MediaRecorder(stream);
      mediaRecorderRef.current = mediaRecorder;
      audioChunksRef.current = [];

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorder.onstop = async () => {
        const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/wav' });
        const url = URL.createObjectURL(audioBlob);
        setAudioURL(url);
        
        // Convert to base64 for API
        const reader = new FileReader();
        reader.onloadend = () => {
          const base64AudioData = reader.result.split(',')[1];
          sendToAPI(base64AudioData);
        };
        reader.readAsDataURL(audioBlob);
      };

      mediaRecorder.start();
      setIsRecording(true);
      
      toast({
        title: "Recording Started",
        description: "Speak clearly about your expense",
      });
    } catch (error) {
      console.error('Error accessing microphone:', error);
      toast({
        title: "Microphone Error",
        description: "Could not access microphone. Please check permissions.",
        variant: "destructive",
      });
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
      setIsRecording(false);
      setIsProcessing(true);
      
      toast({
        title: "Processing",
        description: "Converting speech to text...",
      });
    }
  };

  const sendToAPI = async (audioData) => {
    try {
      const response = await api.post('/expenses/voice-input', {
        audioData: audioData,
        format: 'wav'
      });

      if (response.data && response.data.success) {
        const expense = response.data.data;
        
        toast({
          title: "Success",
          description: `Expense created: ${expense.description} - ₹${expense.amount}`,
        });
        
        if (onExpenseCreated) {
          onExpenseCreated(expense);
        }
        
        // Reset state
        setAudioURL('');
        setIsProcessing(false);
      } else {
        throw new Error(response.data.message || 'Failed to create expense');
      }
    } catch (error) {
      console.error('Voice input error:', error);
      toast({
        title: "Processing Failed",
        description: error.response?.data?.message || "Failed to process voice input",
        variant: "destructive",
      });
      setIsProcessing(false);
    }
  };

  const handleToggleRecording = () => {
    if (isRecording) {
      stopRecording();
    } else {
      startRecording();
    }
  };

  return (
    <Card className="bg-[#1E1E2A] border-[#2C2C3A] shadow-[0_8px_20px_rgba(0,0,0,0.4)] rounded-[16px] hover:shadow-[0_12px_30px_rgba(123,111,201,0.3)] hover:transform hover:translateY-[-4px] transition-all duration-300">
      <CardHeader>
        <CardTitle className="text-white flex items-center gap-2">
          <Mic className="w-5 h-5 text-[#9C90E8]" />
          Voice Input
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="text-center space-y-4">
          {/* Recording Button */}
          <Button
            onClick={handleToggleRecording}
            disabled={isProcessing}
            className={`w-full py-6 rounded-full transition-all duration-300 ${
              isRecording 
                ? 'bg-red-500 hover:bg-red-600 animate-pulse' 
                : 'bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] hover:from-[#6B5FB9] hover:to-[#8C80D8]'
            }`}
          >
            {isProcessing ? (
              <div className="flex items-center justify-center">
                <Loader2 className="w-5 h-5 animate-spin mr-2" />
                Processing...
              </div>
            ) : isRecording ? (
              <div className="flex items-center justify-center">
                <MicOff className="w-5 h-5 mr-2" />
                Stop Recording
              </div>
            ) : (
              <div className="flex items-center justify-center">
                <Mic className="w-5 h-5 mr-2" />
                Start Recording
              </div>
            )}
          </Button>

          {/* Audio Preview */}
          {audioURL && (
            <div className="mt-4">
              <audio 
                controls 
                src={audioURL} 
                className="w-full h-16 bg-[#0F0F12] rounded-lg"
              />
              <p className="text-sm text-gray-400 mt-2">Recording preview - tap to send</p>
            </div>
          )}

          {/* Instructions */}
          <div className="text-left space-y-2 text-sm text-gray-300">
            <p className="flex items-center gap-2">
              <CheckCircle className="w-4 h-4 text-green-400" />
              Say: "I spent 50 dollars on groceries today"
            </p>
            <p className="flex items-center gap-2">
              <CheckCircle className="w-4 h-4 text-green-400" />
              Say: "Paid 25 dollars for lunch"
            </p>
            <p className="flex items-center gap-2">
              <CheckCircle className="w-4 h-4 text-green-400" />
              Say: "Uber ride 30 dollars last night"
            </p>
          </div>

          {/* Status Indicator */}
          <div className="flex items-center justify-center space-x-2 text-sm">
            {isRecording && (
              <div className="flex items-center gap-2 text-red-400">
                <div className="w-2 h-2 bg-red-400 rounded-full animate-pulse"></div>
                <span>Recording...</span>
              </div>
            )}
            {isProcessing && (
              <div className="flex items-center gap-2 text-[#9C90E8]">
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Processing speech...</span>
              </div>
            )}
            {!isRecording && !isProcessing && (
              <div className="flex items-center gap-2 text-gray-400">
                <Mic className="w-4 h-4" />
                <span>Ready to record</span>
              </div>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

export default VoiceInputButton;
