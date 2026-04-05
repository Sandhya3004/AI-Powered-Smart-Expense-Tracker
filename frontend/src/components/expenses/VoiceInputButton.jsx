import React, { useState, useEffect, useRef } from 'react';
import { Mic, MicOff, Volume2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useToast } from '@/hooks/use-toast';
import { api } from '@/api/api';

const VoiceInputButton = ({ onExpenseCreated, className = "" }) => {
  const [isListening, setIsListening] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [transcript, setTranscript] = useState('');
  const recognitionRef = useRef(null);
  const timeoutRef = useRef(null);
  const { toast } = useToast();

  useEffect(() => {
    // Initialize speech recognition
    if (typeof window !== 'undefined') {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      
      if (SpeechRecognition) {
        const recognition = new SpeechRecognition();
        recognition.continuous = false;
        recognition.interimResults = false;
        recognition.lang = 'en-US';
        recognition.maxAlternatives = 1;

        recognition.onstart = () => {
          console.log('Speech recognition started');
          setIsListening(true);
          setIsProcessing(false);
        };

        recognition.onresult = async (event) => {
          const transcript = event.results[0][0].transcript;
          console.log('Speech recognition result:', transcript);
          setTranscript(transcript);
          
          // Auto-stop after getting result
          recognition.stop();
        };

        recognition.onerror = (event) => {
          console.error('Speech recognition error:', event.error);
          setIsListening(false);
          setIsProcessing(false);
          
          let errorMessage = 'Voice recognition failed';
          switch (event.error) {
            case 'no-speech':
              errorMessage = 'No speech detected. Please try again.';
              break;
            case 'audio-capture':
              errorMessage = 'Microphone not available. Please check permissions.';
              break;
            case 'not-allowed':
              errorMessage = 'Microphone permission denied. Please allow microphone access.';
              break;
            case 'network':
              errorMessage = 'Network error. Please check your connection.';
              break;
            default:
              errorMessage = 'Voice recognition error. Please try again.';
          }
          
          toast({
            title: 'Voice Recognition Error',
            description: errorMessage,
            variant: 'destructive',
          });
        };

        recognition.onend = () => {
          console.log('Speech recognition ended');
          setIsListening(false);
          
          // If we have a transcript, process it
          if (transcript.trim()) {
            processVoiceInput(transcript);
          }
        };

        recognitionRef.current = recognition;
      } else {
        console.warn('Speech recognition not supported in this browser');
      }
    }

    return () => {
      // Cleanup
      if (recognitionRef.current) {
        recognitionRef.current.abort();
      }
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, [transcript, toast]);

  const startListening = () => {
    if (!recognitionRef.current) {
      toast({
        title: 'Not Supported',
        description: 'Speech recognition is not supported in your browser',
        variant: 'destructive',
      });
      return;
    }

    try {
      // Reset state
      setTranscript('');
      setIsProcessing(false);
      
      // Start recognition
      recognitionRef.current.start();
      
      // Set a timeout to stop listening after 10 seconds
      timeoutRef.current = setTimeout(() => {
        if (recognitionRef.current && isListening) {
          recognitionRef.current.stop();
          toast({
            title: 'Timeout',
            description: 'Listening timeout. Please try again.',
            variant: 'destructive',
          });
        }
      }, 10000);
      
    } catch (error) {
      console.error('Error starting speech recognition:', error);
      toast({
        title: 'Error',
        description: 'Failed to start voice recognition',
        variant: 'destructive',
      });
    }
  };

  const stopListening = () => {
    if (recognitionRef.current && isListening) {
      recognitionRef.current.stop();
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    }
  };

  const processVoiceInput = async (transcriptText) => {
    setIsProcessing(true);
    
    try {
      console.log('Processing voice input:', transcriptText);
      
      // Send transcript to AI service for processing
      const response = await api.post('/nlp/process-voice', {
        transcription: transcriptText
      });

      if (response.data.success) {
        const expense = response.data.expense;
        
        // Show success message with details
        toast({
          title: 'Expense Created Successfully!',
          description: `${expense.description} - ₹${expense.amount}`,
          duration: 4000,
        });

        // Notify parent component
        if (onExpenseCreated) {
          onExpenseCreated({
            amount: parseFloat(expense.amount),
            description: expense.description || expense.merchant || 'Voice expense',
            category: expense.category || 'Other',
            merchant: expense.merchant || '',
            date: expense.date || new Date().toISOString().split('T')[0],
            source: 'voice',
            status: 'COMPLETED',
          });
        }

        // Trigger refresh for transaction list
        window.dispatchEvent(new CustomEvent('refreshTransactions'));
        
      } else {
        toast({
          title: 'Processing Failed',
          description: response.data.error || 'Failed to process voice input',
          variant: 'destructive',
        });
      }
      
    } catch (error) {
      console.error('Error processing voice input:', error);
      const errorMessage = error.response?.data?.error || 'Failed to process voice input';
      toast({
        title: 'Processing Error',
        description: errorMessage,
        variant: 'destructive',
      });
    } finally {
      setIsProcessing(false);
      setTranscript('');
    }
  };

  const handleClick = () => {
    if (isListening) {
      stopListening();
    } else {
      startListening();
    }
  };

  // Check if browser supports speech recognition
  const isSupported = typeof window !== 'undefined' && 
                     (window.SpeechRecognition || window.webkitSpeechRecognition);

  if (!isSupported) {
    return (
      <Button
        variant="outline"
        size="sm"
        disabled
        className={className}
        title="Voice input not supported in this browser"
      >
        <Mic className="h-4 w-4" />
        Voice
      </Button>
    );
  }

  return (
    <div className="relative">
      <Button
        variant={isListening ? "destructive" : "outline"}
        size="sm"
        onClick={handleClick}
        disabled={isProcessing}
        className={`relative ${className} ${isListening ? 'animate-pulse' : ''}`}
        title={isListening ? "Stop recording" : "Record expense"}
      >
        {isListening ? (
          <>
            <MicOff className="h-4 w-4" />
            <span className="ml-2">Stop</span>
            {/* Visual indicator for recording */}
            <span className="absolute -top-1 -right-1 flex h-3 w-3">
              <span className="animate-ping absolute inline-flex h-3 w-3 rounded-full bg-red-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500"></span>
            </span>
          </>
        ) : (
          <>
            <Mic className="h-4 w-4" />
            <span className="ml-2">Voice</span>
          </>
        )}
      </Button>

      {/* Processing indicator */}
      {isProcessing && (
        <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-90 rounded-md">
          <div className="flex items-center gap-2">
            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
            <span className="text-xs text-blue-600">Processing...</span>
          </div>
        </div>
      )}

      {/* Voice feedback modal */}
      {isListening && (
        <div className="absolute bottom-full left-0 mb-2 p-3 bg-white border border-gray-200 rounded-lg shadow-lg min-w-[200px] z-50">
          <div className="flex items-center gap-2 mb-2">
            <Volume2 className="h-4 w-4 text-red-500 animate-pulse" />
            <span className="text-sm font-medium text-red-600">Listening...</span>
          </div>
          <p className="text-xs text-gray-600 mb-2">
            Speak your expense like: "I spent ₹500 on groceries today"
          </p>
          {transcript && (
            <div className="p-2 bg-gray-50 rounded text-xs">
              <span className="text-gray-500">Heard:</span> {transcript}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default VoiceInputButton;
