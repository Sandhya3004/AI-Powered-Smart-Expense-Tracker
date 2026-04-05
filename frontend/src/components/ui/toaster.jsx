import * as React from "react"
import { cn } from "@/lib/utils"
import { useToast } from "@/hooks/use-toast"

const Toaster = () => {
  const { toasts, dismiss } = useToast()

  return (
    <div className="fixed bottom-4 left-4 z-50 flex flex-col gap-2 p-4">
      {toasts.map(({ id, title, description, variant, action, ...props }) => (
        <div
          key={id}
          className={cn(
            "group pointer-events-auto relative flex w-full max-w-sm items-center justify-between space-x-4 overflow-hidden rounded-xl border p-4 pr-8 shadow-lg animate-slide-in-left",
            variant === "destructive" 
              ? "bg-red-600 border-red-700 text-white" 
              : variant === "success"
              ? "bg-green-600 border-green-700 text-white"
              : "bg-[#2A2540] border-[#3A3560] text-white",
            props.className
          )}
        >
          <div className="grid gap-1">
            {title && <div className="text-sm font-semibold">{title}</div>}
            {description && <div className="text-sm opacity-90">{description}</div>}
          </div>
          {action}
          <button
            onClick={() => dismiss(id)}
            className="absolute right-2 top-2 rounded-md p-1 text-white/70 hover:text-white transition-colors"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
      ))}
    </div>
  )
}

export { Toaster }
