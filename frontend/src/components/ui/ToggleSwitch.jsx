const ToggleSwitch = ({ enabled, onChange, disabled = false }) => {
  return (
    <button
      onClick={() => !disabled && onChange(!enabled)}
      disabled={disabled}
      className={`relative w-14 h-7 rounded-full transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-[#1E1E2A] focus:ring-[#7B6FC9] ${
        enabled 
          ? "bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8]" 
          : "bg-gray-600"
      } ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
      aria-pressed={enabled}
      role="switch"
    >
      <span
        className={`absolute top-1 left-1 w-5 h-5 bg-white rounded-full shadow-md transform transition-all duration-300 ${
          enabled ? "translate-x-7" : "translate-x-0"
        }`}
      />
    </button>
  );
};

export default ToggleSwitch;
